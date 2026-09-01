package mikohime.verify;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ModuleExportNode;
import org.objectweb.asm.tree.ModuleNode;
import org.objectweb.asm.tree.ModuleOpenNode;
import org.objectweb.asm.tree.ModuleProvideNode;
import org.objectweb.asm.tree.ModuleRequireNode;
import org.objectweb.asm.tree.RecordComponentNode;
import org.objectweb.asm.tree.TypeAnnotationNode;

public final class ArtifactVerifier {
    private static final String MANIFEST_PATH = "META-INF/MANIFEST.MF";
    private static final List<ArtifactSpec> UPSTREAM_ARTIFACTS = List.of(
        new ArtifactSpec("commons-compiler-3.0.12.jar", "upstream-resolved", List.of(), List.of()),
        new ArtifactSpec("commons-compiler-jdk-3.0.12.jar", "upstream-resolved", List.of(), List.of()),
        new ArtifactSpec("disruptor-4.0.0.jar", "upstream-resolved", List.of(), List.of()),
        new ArtifactSpec("janino-3.0.12.jar", "upstream-resolved", List.of(), List.of()),
        new ArtifactSpec("jaxb-api-2.4.0-b180830.0359.jar", "upstream-resolved", List.of(), List.of()),
        new ArtifactSpec("log4j-api-3.0.0-alpha1.jar", "upstream-resolved", List.of(), List.of()),
        new ArtifactSpec("log4j-plugins-3.0.0-alpha1.jar", "upstream-resolved", List.of(), List.of()),
        new ArtifactSpec("txw2-3.0.2.jar", "upstream-resolved", List.of(), List.of())
    );

    private static final List<ArtifactSpec> REBUILT_ARTIFACTS = List.of(
        new ArtifactSpec("jcraft-jorbis-0.0.17.jar", "rebuilt-from-recovered-source", List.of(), List.of()),
        new ArtifactSpec("jinput.jar", "rebuilt-from-recovered-source", List.of(), List.of()),
        new ArtifactSpec(
            "log4j-1.2-api-3.0.0-alpha1.jar",
            "upstream-base-with-recompiled-custom-overlay",
            List.of("log4j-api-3.0.0-alpha1.jar", "log4j-core-3.0.0-alpha1.jar", "log4j-plugins-3.0.0-alpha1.jar"),
            combineRules(
                rules(
                    "Optional JMS integration not shipped in the intended flat-folder runtime.",
                    "org/apache/log4j/or/jms/"
                )
            )
        ),
        new ArtifactSpec(
            "log4j-core-3.0.0-alpha1.jar",
            "upstream-base-with-recompiled-custom-overlay",
            List.of("log4j-api-3.0.0-alpha1.jar", "log4j-plugins-3.0.0-alpha1.jar", "disruptor-4.0.0.jar"),
            combineRules(
                rules(
                    "Optional Jackson-based JSON/YAML integration not shipped in the intended flat-folder runtime.",
                    "org/apache/logging/log4j/core/config/json/",
                    "org/apache/logging/log4j/core/config/yaml/",
                    "org/apache/logging/log4j/core/filter/MutableThreadContextMapFilter",
                    "org/apache/logging/log4j/core/parser/AbstractJacksonLogEventParser"
                ),
                rules(
                    "Optional alternate async queue integration not shipped in the intended flat-folder runtime.",
                    "org/apache/logging/log4j/core/async/DisruptorBlockingQueueFactory",
                    "org/apache/logging/log4j/core/async/JCToolsBlockingQueueFactory"
                ),
                rules(
                    "Optional async handler signature tied to unshipped legacy disruptor APIs.",
                    "org/apache/logging/log4j/core/async/AsyncLoggerConfigDisruptor$Log4jEventWrapperHandler",
                    "org/apache/logging/log4j/core/async/RingBufferLogEventHandler"
                ),
                rules(
                    "Optional OSGi integration not shipped in the intended flat-folder runtime.",
                    "org/apache/logging/log4j/core/osgi/"
                ),
                rules(
                    "Optional JAnsi integration not shipped in the intended flat-folder runtime.",
                    "org/apache/logging/log4j/core/pattern/JAnsiTextRenderer"
                ),
                List.of(
                    new ExclusionRule(
                        "org/apache/logging/log4j/core/appender/rolling/action/CommonsCompressAction",
                        "Optional Commons Compress integration not shipped in the intended flat-folder runtime."
                    )
                )
            )
        ),
        new ArtifactSpec(
            "lwjgl.jar",
            "rebuilt-from-recovered-source",
            List.of("jinput.jar"),
            combineRules(
                rules(
                    "macOS-only platform class not resolvable from the shipped Windows runtime.",
                    "org/lwjgl/MacOSXSysImplementation"
                ),
                rules(
                    "OpenGL ES bridge classes depend on compile-time-only stubs intentionally omitted from the shipped desktop runtime.",
                    "org/lwjgl/opengl/ContextGLES",
                    "org/lwjgl/opengl/DrawableGLES",
                    "org/lwjgl/opengl/Display$6"
                )
            )
        ),
        new ArtifactSpec(
            "lwjgl_util.jar",
            "rebuilt-from-recovered-source",
            List.of("lwjgl.jar", "jinput.jar"),
            combineRules(
                rules(
                    "Optional ASM-backed mapped-object transformer support is not shipped as a runtime dependency.",
                    "org/lwjgl/util/mapped/MappedObjectTransformer"
                )
            )
        ),
        new ArtifactSpec(
            "xstream-1.4.21_miko.jar",
            "upstream-base-with-recompiled-custom-overlay",
            List.of(),
            combineRules(
                rules(
                    "Optional XML Pull / MXParser / XPP3 integration not shipped in the intended flat-folder runtime.",
                    "com/thoughtworks/xstream/io/xml/AbstractXpp",
                    "com/thoughtworks/xstream/io/xml/KXml2",
                    "com/thoughtworks/xstream/io/xml/MXParser",
                    "com/thoughtworks/xstream/io/xml/Xpp",
                    "com/thoughtworks/xstream/io/xml/xppdom/"
                ),
                rules(
                    "Optional Dom4J integration not shipped in the intended flat-folder runtime.",
                    "com/thoughtworks/xstream/io/xml/Dom4J"
                ),
                rules(
                    "Optional JDOM integration not shipped in the intended flat-folder runtime.",
                    "com/thoughtworks/xstream/io/xml/JDom"
                ),
                rules(
                    "Optional XOM integration not shipped in the intended flat-folder runtime.",
                    "com/thoughtworks/xstream/io/xml/Xom"
                ),
                rules(
                    "Optional Jettison integration not shipped in the intended flat-folder runtime.",
                    "com/thoughtworks/xstream/io/json/Jettison"
                ),
                rules(
                    "Optional CGLIB integration not shipped in the intended flat-folder runtime.",
                    "com/thoughtworks/xstream/converters/reflection/CGLIB",
                    "com/thoughtworks/xstream/mapper/CGLIB",
                    "com/thoughtworks/xstream/security/CGLIB"
                ),
                List.of(
                    new ExclusionRule(
                        "com/thoughtworks/xstream/io/xml/BEAStaxDriver",
                        "Optional BEA StAX integration not shipped in the intended flat-folder runtime."
                    ),
                    new ExclusionRule(
                        "com/thoughtworks/xstream/io/xml/WstxDriver",
                        "Optional Woodstox integration not shipped in the intended flat-folder runtime."
                    ),
                    new ExclusionRule(
                        "com/thoughtworks/xstream/converters/extended/ActivationDataFlavorConverter",
                        "Optional javax.activation integration not present on JDK 21 and not shipped in the intended flat-folder runtime."
                    ),
                    new ExclusionRule(
                        "com/thoughtworks/xstream/core/util/Base64JAXBCodec",
                        "Optional javax.xml.bind helper is not treated as an isolated XStream dependency in the verifier."
                    ),
                    new ExclusionRule(
                        "com/thoughtworks/xstream/core/util/ISO8601JodaTimeConverter",
                        "Optional Joda-Time integration not shipped in the intended flat-folder runtime."
                    )
                )
            )
        )
    );

    private static final Map<String, ArtifactSpec> REBUILT_BY_NAME = REBUILT_ARTIFACTS.stream()
        .collect(Collectors.toMap(ArtifactSpec::fileName, Function.identity(), (left, right) -> left, LinkedHashMap::new));

    private ArtifactVerifier() {
    }

    public static void main(String[] args) throws Exception {
        Path projectRoot = Path.of(args[0]);
        Path originalDir = Path.of(args[1]);
        Path rebuiltDir = Path.of(args[2]);
        Path jsonReport = Path.of(args[3]);
        Path textReport = textReportPath(jsonReport);
        Map<Path, JarSnapshot> jarSnapshots = new HashMap<>();

        List<FailureDetail> failures = new ArrayList<>();
        Map<String, ArtifactOutcome> outcomes = new LinkedHashMap<>();
        UPSTREAM_ARTIFACTS.forEach(spec -> outcomes.put(spec.fileName(), new ArtifactOutcome(spec.fileName(), spec.status())));
        REBUILT_ARTIFACTS.forEach(spec -> outcomes.put(spec.fileName(), new ArtifactOutcome(spec.fileName(), spec.status())));

        DistributionComparison distribution = compareDistributionFiles(projectRoot, rebuiltDir);
        if (!distribution.matches()) {
            failures.add(new FailureDetail(
                "distribution",
                "expected-files",
                "Distribution file set differs from the intended 15 jars/config/resources/windows natives.",
                distribution.context(),
                null
            ));
        }

        for (ArtifactSpec spec : UPSTREAM_ARTIFACTS) {
            ArtifactOutcome outcome = outcomes.get(spec.fileName());
            Path expected = originalDir.resolve(spec.fileName());
            Path actual = rebuiltDir.resolve(spec.fileName());
            if (!Files.isRegularFile(actual)) {
                outcome.exactBytes = false;
                failures.add(new FailureDetail(spec.fileName(), "exact-bytes", "Output jar is missing.", List.of(actual.toString()), null));
                continue;
            }
            ExactMatch exactMatch = compareExactBytes(expected, actual);
            outcome.exactBytes = exactMatch.matches();
            if (!exactMatch.matches()) {
                failures.add(new FailureDetail(
                    spec.fileName(),
                    "exact-bytes",
                    "Resolved upstream artifact is not byte-identical to the reference jar.",
                    List.of(
                        "expectedSha256=" + exactMatch.expectedSha256(),
                        "actualSha256=" + exactMatch.actualSha256()
                    ),
                    null
                ));
            }
        }

        for (ArtifactSpec spec : REBUILT_ARTIFACTS) {
            ArtifactOutcome outcome = outcomes.get(spec.fileName());
            Path expected = originalDir.resolve(spec.fileName());
            Path actual = rebuiltDir.resolve(spec.fileName());
            if (!Files.isRegularFile(actual)) {
                failures.add(new FailureDetail(spec.fileName(), "jar-comparison", "Output jar is missing.", List.of(actual.toString()), null));
                outcome.classInventory = false;
                outcome.classMetadata = false;
                outcome.manifest = false;
                outcome.resources = false;
                continue;
            }
            JarComparison comparison = compareJars(spec, expected, actual, jarSnapshots);
            outcome.classInventory = comparison.classInventory().matches();
            outcome.classMetadata = comparison.classMetadata().matches();
            outcome.manifest = comparison.manifest().matches();
            outcome.resources = comparison.resources().matches();
            if (!comparison.duplicateEntries().matches()) {
                failures.add(new FailureDetail(spec.fileName(), "duplicate-entries", "Jar contains duplicate entries.", comparison.duplicateEntries().context(), null));
            }
            if (!comparison.classInventory().matches()) {
                failures.add(new FailureDetail(spec.fileName(), "class-inventory", "Class inventory differs.", comparison.classInventory().context(), null));
            }
            if (!comparison.classMetadata().matches()) {
                failures.add(new FailureDetail(spec.fileName(), "class-metadata", "Class metadata differs.", comparison.classMetadata().context(), null));
            }
            if (!comparison.manifest().matches()) {
                failures.add(new FailureDetail(spec.fileName(), "manifest", "Manifest differs after signature normalization.", comparison.manifest().context(), null));
            }
            if (!comparison.resources().matches()) {
                failures.add(new FailureDetail(spec.fileName(), "resources", "Resources differ.", comparison.resources().context(), null));
            }
            LinkageResult linkage = verifyLinkage(spec, rebuiltDir, jarSnapshots);
            outcome.linkage = linkage.matches();
            outcome.linkageCheckedClasses = linkage.checkedClasses();
            outcome.linkageExcludedClasses = linkage.excludedClasses();
            outcome.linkageExclusions = linkage.exclusionSummary();
            if (!linkage.matches()) {
                failures.add(new FailureDetail(
                    spec.fileName(),
                    "linkage",
                    "Failed to load rebuilt classes or resolve declared members in the isolated artifact classloader.",
                    linkage.context(),
                    linkage.stackTrace()
                ));
            }
        }

        compareProbe("jcraft-jorbis-0.0.17.jar", originalDir, rebuiltDir, ArtifactVerifier::jorbisProbe, outcomes, failures);
        compareProbe("jinput.jar", originalDir, rebuiltDir, ArtifactVerifier::jinputProbe, outcomes, failures);
        compareProbe("lwjgl.jar", originalDir, rebuiltDir, ArtifactVerifier::lwjglProbe, outcomes, failures);
        compareProbe("lwjgl_util.jar", originalDir, rebuiltDir, ArtifactVerifier::lwjglUtilProbe, outcomes, failures);
        compareProbe(
            "log4j-core-3.0.0-alpha1.jar",
            originalDir,
            rebuiltDir,
            loader -> pluginProbe(loader, "org.apache.logging.log4j.core.plugins.Log4jPlugins"),
            outcomes,
            failures
        );
        compareProbe(
            "log4j-1.2-api-3.0.0-alpha1.jar",
            originalDir,
            rebuiltDir,
            loader -> pluginProbe(loader, "org.apache.log4j.plugins.Log4jPlugins"),
            outcomes,
            failures
        );
        compareProbe("xstream-1.4.21_miko.jar", originalDir, rebuiltDir, ArtifactVerifier::xstreamProbe, outcomes, failures);
        verifyNativeLwjglProbe(rebuiltDir, outcomes.get("lwjgl.jar"), failures);

        Files.createDirectories(jsonReport.getParent());
        Files.writeString(jsonReport, buildJsonReport(distribution, outcomes.values(), failures));
        Files.writeString(textReport, buildTextReport(distribution, outcomes.values(), failures, jsonReport, textReport));

        if (!failures.isEmpty()) {
            failures.forEach(failure -> System.err.println("FAIL: " + failure.artifact() + " [" + failure.check() + "] " + failure.message()));
            throw new IllegalStateException("Equivalence verification failed; see " + jsonReport + " and " + textReport);
        }
        System.out.println("Verified 15 artifacts plus configuration/resources/windows natives with metadata, linkage, and isolated probes.");
        System.out.println("Reports: " + jsonReport.toAbsolutePath() + " and " + textReport.toAbsolutePath());
    }

    private static JarComparison compareJars(
        ArtifactSpec artifact,
        Path expectedJar,
        Path actualJar,
        Map<Path, JarSnapshot> jarSnapshots
    ) throws Exception {
        JarSnapshot expected = scanJar(expectedJar, jarSnapshots);
        JarSnapshot actual = scanJar(actualJar, jarSnapshots);
        List<ExclusionRule> comparisonExclusions = comparisonExclusions(artifact);
        Map<String, String> expectedClasses = comparableClasses(expected.classes(), comparisonExclusions);
        Map<String, String> actualClasses = comparableClasses(actual.classes(), comparisonExclusions);
        MatchResult duplicateEntries = actual.duplicateEntries().isEmpty()
            ? MatchResult.success()
            : new MatchResult(false, actual.duplicateEntries().stream().sorted().toList());
        MatchResult classInventory = compareSets(expectedClasses.keySet(), actualClasses.keySet());
        MatchResult classMetadata = compareNamedValues(expectedClasses, actualClasses);
        MatchResult manifest = compareTextValue(expected.manifest(), actual.manifest(), MANIFEST_PATH);
        MatchResult resources = compareNamedValues(expected.resources(), actual.resources());
        return new JarComparison(duplicateEntries, classInventory, classMetadata, manifest, resources);
    }

    private static JarSnapshot scanJar(Path path, Map<Path, JarSnapshot> jarSnapshots) throws Exception {
        Path jarPath = path.toAbsolutePath().normalize();
        JarSnapshot cached = jarSnapshots.get(jarPath);
        if (cached != null) {
            return cached;
        }
        Map<String, Integer> entryCounts = new HashMap<>();
        Map<String, String> classes = new TreeMap<>();
        Map<String, String> resources = new TreeMap<>();
        String manifest = null;
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            var entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                String name = entry.getName();
                entryCounts.merge(name, 1, Integer::sum);
                try (InputStream input = jar.getInputStream(entry)) {
                    byte[] bytes = input.readAllBytes();
                    if (MANIFEST_PATH.equals(name)) {
                        manifest = canonicalManifest(bytes);
                    } else if (name.endsWith(".class")) {
                        String className = new ClassReader(bytes).getClassName();
                        classes.put(className, fingerprintClass(bytes));
                    } else if (!isSignatureArtifact(name)) {
                        resources.put(name, hex(hash(bytes)));
                    }
                }
            }
        }
        Set<String> duplicates = entryCounts.entrySet().stream()
            .filter(entry -> entry.getValue() > 1)
            .map(Map.Entry::getKey)
            .collect(Collectors.toCollection(TreeSet::new));
        JarSnapshot snapshot = new JarSnapshot(classes, manifest, resources, duplicates);
        jarSnapshots.put(jarPath, snapshot);
        return snapshot;
    }

    private static String fingerprintClass(byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

        List<String> lines = new ArrayList<>();
        lines.add("class|access=" + node.access + "|name=" + safe(node.name)
            + "|signature=" + safe(node.signature) + "|super=" + safe(node.superName)
            + "|interfaces=" + joinList(node.interfaces));
        if (node.outerClass != null || node.outerMethod != null || node.outerMethodDesc != null) {
            lines.add("outer|class=" + safe(node.outerClass) + "|method=" + safe(node.outerMethod)
                + "|desc=" + safe(node.outerMethodDesc));
        }
        sortedStrings(node.permittedSubclasses).forEach(item -> lines.add("permitted|" + item));
        sortedInnerClasses(node.innerClasses).stream()
            .filter(inner -> isRelevantInnerEntry(node.name, inner))
            .forEach(inner ->
                lines.add("inner|name=" + safe(inner.name) + "|outer=" + safe(inner.outerName)
                    + "|inner=" + safe(inner.innerName)));
        addAnnotationLines(lines, "class-visible", node.visibleAnnotations);
        addTypeAnnotationLines(lines, "class-visible-type", node.visibleTypeAnnotations);
        addAnnotationLines(lines, "class-invisible", node.invisibleAnnotations);
        addTypeAnnotationLines(lines, "class-invisible-type", node.invisibleTypeAnnotations);
        if (node.module != null) {
            addModuleLines(lines, node.module);
        }
        if (node.recordComponents != null) {
            for (int index = 0; index < node.recordComponents.size(); index++) {
                RecordComponentNode component = node.recordComponents.get(index);
                String key = "record[" + index + "]|name=" + component.name + "|desc=" + component.descriptor
                    + "|signature=" + safe(component.signature);
                lines.add(key);
                addAnnotationLines(lines, key + "|visible", component.visibleAnnotations);
                addTypeAnnotationLines(lines, key + "|visible-type", component.visibleTypeAnnotations);
                addAnnotationLines(lines, key + "|invisible", component.invisibleAnnotations);
                addTypeAnnotationLines(lines, key + "|invisible-type", component.invisibleTypeAnnotations);
            }
        }
        sortedFields(node.fields).stream()
            .filter(field -> shouldCompareField(field.name, field.access))
            .forEach(field -> {
            String key = "field|name=" + field.name + "|desc=" + field.desc + "|signature=" + safe(field.signature)
                + "|access=" + field.access + "|value=" + canonicalValue(field.value);
            lines.add(key);
            addAnnotationLines(lines, key + "|visible", field.visibleAnnotations);
            addTypeAnnotationLines(lines, key + "|visible-type", field.visibleTypeAnnotations);
            addAnnotationLines(lines, key + "|invisible", field.invisibleAnnotations);
            addTypeAnnotationLines(lines, key + "|invisible-type", field.invisibleTypeAnnotations);
        });
        sortedMethods(node.methods).stream()
            .filter(method -> shouldCompareMethod(method.name, method.access))
            .forEach(method -> {
            String key = "method|name=" + method.name + "|desc=" + method.desc + "|signature=" + safe(method.signature)
                + "|access=" + method.access + "|exceptions=" + joinList(method.exceptions);
            lines.add(key);
            if (method.annotationDefault != null) {
                lines.add(key + "|default=" + canonicalValue(method.annotationDefault));
            }
            addAnnotationLines(lines, key + "|visible", method.visibleAnnotations);
            addTypeAnnotationLines(lines, key + "|visible-type", method.visibleTypeAnnotations);
            addAnnotationLines(lines, key + "|invisible", method.invisibleAnnotations);
            addTypeAnnotationLines(lines, key + "|invisible-type", method.invisibleTypeAnnotations);
            addParameterAnnotationLines(lines, key + "|parameter-visible", method.visibleParameterAnnotations);
            addParameterAnnotationLines(lines, key + "|parameter-invisible", method.invisibleParameterAnnotations);
        });
        return String.join("\n", lines);
    }

    private static void addModuleLines(List<String> lines, ModuleNode module) {
        lines.add("module|name=" + safe(module.name) + "|access=" + module.access + "|version=" + safe(module.version));
        if (module.mainClass != null) {
            lines.add("module-main-class|" + module.mainClass);
        }
        sortedStrings(module.packages).forEach(item -> lines.add("module-package|" + item));
        sortedStrings(module.uses).forEach(item -> lines.add("module-uses|" + item));
        sortedModuleRequires(module.requires).forEach(require ->
            lines.add("module-requires|module=" + require.module + "|access=" + require.access + "|version=" + safe(require.version)));
        sortedModuleExports(module.exports).forEach(export ->
            lines.add("module-exports|package=" + export.packaze + "|access=" + export.access + "|modules=" + joinList(export.modules)));
        sortedModuleOpens(module.opens).forEach(open ->
            lines.add("module-opens|package=" + open.packaze + "|access=" + open.access + "|modules=" + joinList(open.modules)));
        sortedModuleProvides(module.provides).forEach(provide ->
            lines.add("module-provides|service=" + provide.service + "|providers=" + joinList(provide.providers)));
    }

    private static void addAnnotationLines(List<String> lines, String prefix, List<? extends AnnotationNode> annotations) {
        if (annotations == null) {
            return;
        }
        annotations.stream()
            .map(ArtifactVerifier::annotationFingerprint)
            .sorted()
            .forEach(annotation -> lines.add(prefix + "|" + annotation));
    }

    private static void addTypeAnnotationLines(List<String> lines, String prefix, List<? extends TypeAnnotationNode> annotations) {
        if (annotations == null) {
            return;
        }
        annotations.stream()
            .map(ArtifactVerifier::typeAnnotationFingerprint)
            .sorted()
            .forEach(annotation -> lines.add(prefix + "|" + annotation));
    }

    private static void addParameterAnnotationLines(List<String> lines, String prefix, List<? extends AnnotationNode>[] parameterAnnotations) {
        if (parameterAnnotations == null) {
            return;
        }
        for (int index = 0; index < parameterAnnotations.length; index++) {
            List<? extends AnnotationNode> annotations = parameterAnnotations[index];
            if (annotations == null) {
                continue;
            }
            final int parameterIndex = index;
            annotations.stream()
                .map(ArtifactVerifier::annotationFingerprint)
                .sorted()
                .forEach(annotation -> lines.add(prefix + "[" + parameterIndex + "]|" + annotation));
        }
    }

    private static String annotationFingerprint(AnnotationNode annotation) {
        return annotation.desc + canonicalAnnotationValues(annotation.values);
    }

    private static Map<String, String> comparableClasses(Map<String, String> classes, List<ExclusionRule> exclusions) {
        return classes.entrySet().stream()
            .filter(entry -> !isClassMetadataExcluded(entry.getKey(), exclusions))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> left, TreeMap::new));
    }

    private static String typeAnnotationFingerprint(TypeAnnotationNode annotation) {
        return "typeRef=" + annotation.typeRef + "|typePath=" + safe(annotation.typePath == null ? null : annotation.typePath.toString())
            + "|" + annotationFingerprint(annotation);
    }

    private static List<ExclusionRule> comparisonExclusions(ArtifactSpec artifact) {
        if ("jinput.jar".equals(artifact.fileName())) {
            return combineRules(
                artifact.linkageExclusions(),
                rules(
                    "Compiler-emitted shutdown-hook helper constructors are allowed to vary.",
                    "net/java/games/input/DirectInputEnvironmentPlugin$ShutdownHook",
                    "net/java/games/input/LinuxEnvironmentPlugin$ShutdownHook",
                    "net/java/games/input/WinTabEnvironmentPlugin$ShutdownHook"
                )
            );
        }
        return artifact.linkageExclusions();
    }

    private static String canonicalAnnotationValues(List<Object> values) {
        if (values == null || values.isEmpty()) {
            return "{}";
        }
        Map<String, String> namedValues = new TreeMap<>();
        for (int index = 0; index < values.size(); index += 2) {
            String name = String.valueOf(values.get(index));
            Object value = values.get(index + 1);
            namedValues.put(name, canonicalValue(value));
        }
        return namedValues.entrySet().stream()
            .map(entry -> entry.getKey() + "=" + entry.getValue())
            .collect(Collectors.joining(",", "{", "}"));
    }

    private static String canonicalValue(Object value) {
        if (value == null) {
            return "<null>";
        }
        if (value instanceof String text) {
            return quote(text);
        }
        if (value instanceof Type type) {
            return "type(" + type.getDescriptor() + ")";
        }
        if (value instanceof AnnotationNode annotation) {
            return annotationFingerprint(annotation);
        }
        if (value instanceof List<?> list) {
            return list.stream().map(ArtifactVerifier::canonicalValue).collect(Collectors.joining(",", "[", "]"));
        }
        if (value instanceof String[] enumValue && enumValue.length == 2) {
            return "enum(" + enumValue[0] + ":" + enumValue[1] + ")";
        }
        if (value instanceof byte[] items) {
            return Arrays.toString(items);
        }
        if (value instanceof short[] items) {
            return Arrays.toString(items);
        }
        if (value instanceof int[] items) {
            return Arrays.toString(items);
        }
        if (value instanceof long[] items) {
            return Arrays.toString(items);
        }
        if (value instanceof float[] items) {
            return Arrays.toString(items);
        }
        if (value instanceof double[] items) {
            return Arrays.toString(items);
        }
        if (value instanceof char[] items) {
            return Arrays.toString(items);
        }
        if (value instanceof boolean[] items) {
            return Arrays.toString(items);
        }
        if (value instanceof Object[] items) {
            return Arrays.stream(items).map(ArtifactVerifier::canonicalValue).collect(Collectors.joining(",", "[", "]"));
        }
        return String.valueOf(value);
    }

    private static String canonicalManifest(byte[] bytes) throws IOException {
        Manifest manifest = new Manifest(new ByteArrayInputStream(bytes));
        List<String> lines = new ArrayList<>();
        appendManifestAttributes(lines, "main", manifest.getMainAttributes());
        manifest.getEntries().entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> appendManifestAttributes(lines, "entry:" + entry.getKey(), entry.getValue()));
        return String.join("\n", lines);
    }

    private static void appendManifestAttributes(List<String> lines, String prefix, Attributes attributes) {
        attributes.entrySet().stream()
            .map(entry -> Map.entry(entry.getKey().toString(), String.valueOf(entry.getValue())))
            .filter(entry -> !isSignatureAttribute(entry.getKey()))
            .sorted(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER))
            .map(entry -> prefix + "|" + entry.getKey() + "=" + entry.getValue())
            .forEach(lines::add);
    }

    private static DistributionComparison compareDistributionFiles(Path projectRoot, Path rebuiltDir) throws IOException {
        Set<String> expected = new TreeSet<>();
        Stream.concat(UPSTREAM_ARTIFACTS.stream(), REBUILT_ARTIFACTS.stream())
            .map(ArtifactSpec::fileName)
            .forEach(expected::add);
        Path distribution = projectRoot.resolve("distribution");
        collectFiles(distribution.resolve("configuration"), expected::add);
        collectFiles(distribution.resolve("resources"), expected::add);
        collectFiles(distribution.resolve("windows"), relative -> expected.add("windows/" + relative));

        Set<String> actual = new TreeSet<>();
        try (Stream<Path> paths = Files.walk(rebuiltDir)) {
            paths.filter(Files::isRegularFile)
                .forEach(path -> actual.add(normalize(rebuiltDir.relativize(path))));
        }

        Set<String> missing = new TreeSet<>(expected);
        missing.removeAll(actual);
        Set<String> unexpected = new TreeSet<>(actual);
        unexpected.removeAll(expected);
        boolean matches = missing.isEmpty() && unexpected.isEmpty();
        List<String> context = new ArrayList<>();
        if (!missing.isEmpty()) {
            context.add("missing=" + limited(missing));
        }
        if (!unexpected.isEmpty()) {
            context.add("unexpected=" + limited(unexpected));
        }
        context.add("expectedCount=" + expected.size());
        context.add("actualCount=" + actual.size());
        return new DistributionComparison(matches, expected.size(), actual.size(), context);
    }

    private static void collectFiles(Path root, Consumer<String> consumer) throws IOException {
        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(Files::isRegularFile)
                .map(path -> normalize(root.relativize(path)))
                .sorted()
                .forEach(consumer);
        }
    }

    private static ExactMatch compareExactBytes(Path expected, Path actual) throws Exception {
        String expectedSha = hex(hash(Files.readAllBytes(expected)));
        String actualSha = hex(hash(Files.readAllBytes(actual)));
        return new ExactMatch(expectedSha.equals(actualSha), expectedSha, actualSha);
    }

    private static LinkageResult verifyLinkage(ArtifactSpec artifact, Path rebuiltDir, Map<Path, JarSnapshot> jarSnapshots) {
        try {
            JarSnapshot snapshot = scanJar(rebuiltDir.resolve(artifact.fileName()), jarSnapshots);
            List<LinkageFailure> failures = new ArrayList<>();
            int checkedClasses = 0;
            int excludedClasses = 0;
            try (URLClassLoader loader = newArtifactClassLoader(rebuiltDir, artifact)) {
                for (String className : snapshot.classes().keySet()) {
                    if ("module-info".equals(className)) {
                        continue;
                    }
                    ExclusionRule exclusion = findExclusion(artifact.linkageExclusions(), className);
                    if (exclusion != null) {
                        excludedClasses++;
                        continue;
                    }
                    try {
                        Class<?> type = Class.forName(toBinaryName(className), false, loader);
                        resolveType(type);
                        checkedClasses++;
                    } catch (Throwable error) {
                        failures.add(new LinkageFailure(className, error));
                    }
                }
            }
            if (failures.isEmpty()) {
                return new LinkageResult(true, checkedClasses, excludedClasses, summarizeExclusions(artifact.linkageExclusions()), List.of(), null);
            }
            List<String> context = new ArrayList<>();
            context.add("checkedClasses=" + checkedClasses);
            context.add("excludedClasses=" + excludedClasses);
            context.add("excluded=" + summarizeExclusions(artifact.linkageExclusions()));
            failures.stream().limit(10).forEach(failure -> context.add(failure.className() + " -> " + rootMessage(failure.error())));
            String stackTrace = failures.stream().limit(5)
                .map(failure -> "Class: " + failure.className() + System.lineSeparator() + stackTrace(failure.error()))
                .collect(Collectors.joining(System.lineSeparator() + "---" + System.lineSeparator()));
            return new LinkageResult(false, checkedClasses, excludedClasses, summarizeExclusions(artifact.linkageExclusions()), context, stackTrace);
        } catch (Throwable error) {
            return new LinkageResult(false, 0, 0, summarizeExclusions(artifact.linkageExclusions()), List.of(rootMessage(error)), stackTrace(error));
        }
    }

    private static void resolveType(Class<?> type) {
        type.getDeclaredAnnotations();
        type.getTypeParameters();
        type.getInterfaces();
        type.getGenericInterfaces();
        type.getSuperclass();
        type.getGenericSuperclass();
        type.getNestHost();
        type.getNestMembers();
        type.getPermittedSubclasses();
        RecordComponent[] recordComponents = type.getRecordComponents();
        if (recordComponents != null) {
            for (RecordComponent component : recordComponents) {
                component.getDeclaredAnnotations();
                component.getGenericType();
                component.getType();
                component.getAccessor();
                resolveAnnotatedType(component.getAnnotatedType());
            }
        }
        for (Field field : type.getDeclaredFields()) {
            field.getDeclaredAnnotations();
            field.getType();
            field.getGenericType();
            resolveAnnotatedType(field.getAnnotatedType());
        }
        for (Constructor<?> constructor : type.getDeclaredConstructors()) {
            constructor.getDeclaredAnnotations();
            constructor.getTypeParameters();
            constructor.getExceptionTypes();
            constructor.getGenericExceptionTypes();
            constructor.getParameterTypes();
            constructor.getGenericParameterTypes();
            constructor.getParameterAnnotations();
            resolveAnnotatedTypeArray(constructor.getAnnotatedExceptionTypes());
            resolveAnnotatedTypeArray(constructor.getAnnotatedParameterTypes());
            resolveAnnotatedType(constructor.getAnnotatedReceiverType());
        }
        for (Method method : type.getDeclaredMethods()) {
            method.getDeclaredAnnotations();
            method.getDefaultValue();
            method.getTypeParameters();
            method.getReturnType();
            method.getGenericReturnType();
            method.getExceptionTypes();
            method.getGenericExceptionTypes();
            method.getParameterTypes();
            method.getGenericParameterTypes();
            method.getParameterAnnotations();
            resolveAnnotatedType(method.getAnnotatedReturnType());
            resolveAnnotatedTypeArray(method.getAnnotatedExceptionTypes());
            resolveAnnotatedTypeArray(method.getAnnotatedParameterTypes());
            resolveAnnotatedType(method.getAnnotatedReceiverType());
        }
    }

    private static void resolveAnnotatedType(AnnotatedType type) {
        if (type == null) {
            return;
        }
        type.getAnnotations();
        type.getType();
    }

    private static void resolveAnnotatedTypeArray(AnnotatedType[] types) {
        if (types == null) {
            return;
        }
        for (AnnotatedType type : types) {
            resolveAnnotatedType(type);
        }
    }

    private static URLClassLoader newArtifactClassLoader(Path directory, ArtifactSpec artifact) throws IOException {
        LinkedHashSet<URL> urls = new LinkedHashSet<>();
        urls.add(requiredUrl(directory.resolve(artifact.fileName())));
        for (String dependency : artifact.runtimeDependencies()) {
            urls.add(requiredUrl(directory.resolve(dependency)));
        }
        return new URLClassLoader(urls.toArray(URL[]::new), ClassLoader.getPlatformClassLoader());
    }

    private static URL requiredUrl(Path path) throws IOException {
        if (!Files.isRegularFile(path)) {
            throw new IOException("Missing dependency jar: " + path);
        }
        return path.toUri().toURL();
    }

    private static void compareProbe(
        String artifactName,
        Path originalDir,
        Path rebuiltDir,
        Probe probe,
        Map<String, ArtifactOutcome> outcomes,
        List<FailureDetail> failures
    ) {
        ArtifactSpec artifact = REBUILT_BY_NAME.get(artifactName);
        ArtifactOutcome outcome = outcomes.get(artifactName);
        try {
            String expected = withArtifactLoader(originalDir, artifact, probe);
            String actual = withArtifactLoader(rebuiltDir, artifact, probe);
            boolean matches = expected.equals(actual);
            outcome.probe = outcome.probe == null ? matches : outcome.probe && matches;
            if (!matches) {
                failures.add(new FailureDetail(
                    artifactName,
                    "probe",
                    "Isolated runtime probe result differs from the original artifact.",
                    List.of("expected=" + expected, "actual=" + actual),
                    null
                ));
            }
        } catch (Throwable error) {
            outcome.probe = false;
            failures.add(new FailureDetail(
                artifactName,
                "probe",
                "Isolated runtime probe failed.",
                List.of(rootMessage(error)),
                stackTrace(error)
            ));
        }
    }

    private static void verifyNativeLwjglProbe(Path rebuiltDir, ArtifactOutcome outcome, List<FailureDetail> failures) {
        ArtifactSpec artifact = REBUILT_BY_NAME.get("lwjgl.jar");
        String previousPath = System.getProperty("org.lwjgl.librarypath");
        try {
            System.setProperty("org.lwjgl.librarypath", rebuiltDir.resolve("windows").toAbsolutePath().toString());
            String result = withArtifactLoader(rebuiltDir, artifact, loader -> {
                Class<?> sys = Class.forName("org.lwjgl.Sys", true, loader);
                Object version = sys.getMethod("getVersion").invoke(null);
                Object is64Bit = sys.getMethod("is64Bit").invoke(null);
                Class.forName("org.lwjgl.opengl.WindowsDisplay", true, loader);
                return version + ":" + is64Bit;
            });
            boolean matches = "2.9.5:true".equals(result);
            outcome.probe = outcome.probe == null ? matches : outcome.probe && matches;
            if (!matches) {
                failures.add(new FailureDetail(
                    "lwjgl.jar",
                    "native-probe",
                    "LWJGL native initialization returned an unexpected result.",
                    List.of("actual=" + result, "expected=2.9.5:true"),
                    null
                ));
            }
        } catch (Throwable error) {
            outcome.probe = false;
            failures.add(new FailureDetail(
                "lwjgl.jar",
                "native-probe",
                "LWJGL native initialization failed.",
                List.of(rootMessage(error)),
                stackTrace(error)
            ));
        } finally {
            if (previousPath == null) {
                System.clearProperty("org.lwjgl.librarypath");
            } else {
                System.setProperty("org.lwjgl.librarypath", previousPath);
            }
        }
    }

    private static String withArtifactLoader(Path directory, ArtifactSpec artifact, Probe probe) throws Exception {
        try (URLClassLoader loader = newArtifactClassLoader(directory, artifact)) {
            return probe.run(loader);
        }
    }

    private static String jorbisProbe(ClassLoader loader) throws Exception {
        Class<?> type = loader.loadClass("com.jcraft.jorbis.Info");
        Object info = type.getConstructor().newInstance();
        type.getMethod("init").invoke(info);
        return type.getField("rate").get(info) + ":" + type.getField("channels").get(info);
    }

    private static String jinputProbe(ClassLoader loader) throws Exception {
        return String.valueOf(loader.loadClass("net.java.games.input.Version").getMethod("getVersion").invoke(null));
    }

    private static String lwjglProbe(ClassLoader loader) throws Exception {
        ByteBuffer buffer = (ByteBuffer) loader.loadClass("org.lwjgl.BufferUtils").getMethod("createByteBuffer", int.class).invoke(null, 16);
        resolveType(loader.loadClass("org.lwjgl.opengl.Display"));
        resolveType(loader.loadClass("org.lwjgl.WindowsSysImplementation"));
        return buffer.capacity() + ":" + buffer.isDirect();
    }

    private static String lwjglUtilProbe(ClassLoader loader) throws Exception {
        Class<?> type = loader.loadClass("org.lwjgl.util.vector.Vector3f");
        Object vector = type.getConstructor(float.class, float.class, float.class).newInstance(1f, 2f, 3f);
        return String.valueOf(type.getMethod("lengthSquared").invoke(vector));
    }

    private static String pluginProbe(ClassLoader loader, String className) throws Exception {
        Object service = loader.loadClass(className).getConstructor().newInstance();
        Object entries = service.getClass().getMethod("getEntries").invoke(service);
        List<String> values = new ArrayList<>();
        for (int index = 0; index < java.lang.reflect.Array.getLength(entries); index++) {
            Object entry = java.lang.reflect.Array.get(entries, index);
            List<String> fields = new ArrayList<>();
            for (String getter : List.of("getKey", "getClassName", "getName", "getNamespace", "getElementType", "isPrintable", "isDeferChildren")) {
                try {
                    fields.add(String.valueOf(entry.getClass().getMethod(getter).invoke(entry)));
                } catch (NoSuchMethodException ignored) {
                    fields.add("-");
                }
            }
            values.add(String.join("|", fields));
        }
        return String.join("\n", values);
    }

    private static String xstreamProbe(ClassLoader loader) throws Exception {
        Class<?> driverType = loader.loadClass("com.thoughtworks.xstream.io.HierarchicalStreamDriver");
        Object driver = loader.loadClass("com.thoughtworks.xstream.io.xml.DomDriver").getConstructor().newInstance();
        Class<?> xstreamType = loader.loadClass("com.thoughtworks.xstream.XStream");
        Constructor<?> constructor = xstreamType.getConstructor(driverType);
        Object xstream = constructor.newInstance(driver);
        String xml = String.valueOf(xstreamType.getMethod("toXML", Object.class).invoke(xstream, "mikohime"));
        Object value = xstreamType.getMethod("fromXML", String.class).invoke(xstream, xml);
        return xml + "|" + value;
    }

    private static MatchResult compareSets(Set<String> expected, Set<String> actual) {
        Set<String> missing = new TreeSet<>(expected);
        missing.removeAll(actual);
        Set<String> added = new TreeSet<>(actual);
        added.removeAll(expected);
        if (missing.isEmpty() && added.isEmpty()) {
            return MatchResult.success();
        }
        List<String> context = new ArrayList<>();
        if (!missing.isEmpty()) {
            context.add("missing=" + limited(missing));
        }
        if (!added.isEmpty()) {
            context.add("added=" + limited(added));
        }
        return new MatchResult(false, context);
    }

    private static MatchResult compareNamedValues(Map<String, String> expected, Map<String, String> actual) {
        Set<String> missing = new TreeSet<>(expected.keySet());
        missing.removeAll(actual.keySet());
        Set<String> added = new TreeSet<>(actual.keySet());
        added.removeAll(expected.keySet());
        List<String> changed = expected.keySet().stream()
            .filter(actual::containsKey)
            .filter(key -> !Objects.equals(expected.get(key), actual.get(key)))
            .sorted()
            .toList();
        if (missing.isEmpty() && added.isEmpty() && changed.isEmpty()) {
            return MatchResult.success();
        }
        List<String> context = new ArrayList<>();
        if (!missing.isEmpty()) {
            context.add("missing=" + limited(missing));
        }
        if (!added.isEmpty()) {
            context.add("added=" + limited(added));
        }
        if (!changed.isEmpty()) {
            String first = changed.get(0);
            context.add("changed=" + limited(changed));
            context.add("firstChanged=" + first);
            context.addAll(diffLines(expected.get(first), actual.get(first)));
        }
        return new MatchResult(false, context);
    }

    private static MatchResult compareTextValue(String expected, String actual, String label) {
        if (Objects.equals(expected, actual)) {
            return MatchResult.success();
        }
        List<String> context = new ArrayList<>();
        context.add("entry=" + label);
        context.addAll(diffLines(expected, actual));
        return new MatchResult(false, context);
    }

    private static List<String> diffLines(String expected, String actual) {
        Set<String> expectedOnly = new TreeSet<>(splitLines(expected));
        expectedOnly.removeAll(splitLines(actual));
        Set<String> actualOnly = new TreeSet<>(splitLines(actual));
        actualOnly.removeAll(splitLines(expected));
        List<String> context = new ArrayList<>();
        if (!expectedOnly.isEmpty()) {
            context.add("expectedOnly=" + limited(expectedOnly));
        }
        if (!actualOnly.isEmpty()) {
            context.add("actualOnly=" + limited(actualOnly));
        }
        return context;
    }

    private static List<String> splitLines(String text) {
        if (text == null || text.isBlank()) {
            return List.of("<empty>");
        }
        return text.lines().toList();
    }

    private static ExclusionRule findExclusion(List<ExclusionRule> exclusions, String className) {
        for (ExclusionRule exclusion : exclusions) {
            if (className.startsWith(exclusion.prefix())) {
                return exclusion;
            }
        }
        return null;
    }

    private static List<String> summarizeExclusions(List<ExclusionRule> exclusions) {
        return exclusions.stream()
            .map(exclusion -> exclusion.prefix() + " => " + exclusion.reason())
            .toList();
    }

    private static String buildJsonReport(
        DistributionComparison distribution,
        Collection<ArtifactOutcome> outcomes,
        List<FailureDetail> failures
    ) {
        String artifactJson = outcomes.stream()
            .sorted(Comparator.comparing(outcome -> outcome.fileName))
            .map(ArtifactOutcome::toJson)
            .collect(Collectors.joining(",\n"));
        String failureJson = failures.stream().map(ArtifactVerifier::failureToJson).collect(Collectors.joining(",\n"));
        return "{\n"
            + "  \"passed\": " + failures.isEmpty() + ",\n"
            + "  \"distributionFiles\": {\n"
            + "    \"matched\": " + distribution.matches() + ",\n"
            + "    \"expectedCount\": " + distribution.expectedCount() + ",\n"
            + "    \"actualCount\": " + distribution.actualCount() + ",\n"
            + "    \"context\": " + jsonArray(distribution.context()) + "\n"
            + "  },\n"
            + "  \"artifacts\": [\n" + artifactJson + "\n  ],\n"
            + "  \"failures\": [\n" + failureJson + "\n  ]\n"
            + "}\n";
    }

    private static String buildTextReport(
        DistributionComparison distribution,
        Collection<ArtifactOutcome> outcomes,
        List<FailureDetail> failures,
        Path jsonReport,
        Path textReport
    ) {
        StringBuilder builder = new StringBuilder();
        builder.append("Verification passed: ").append(failures.isEmpty()).append(System.lineSeparator());
        builder.append("Distribution files matched: ").append(distribution.matches())
            .append(" (expected ").append(distribution.expectedCount())
            .append(", actual ").append(distribution.actualCount()).append(")")
            .append(System.lineSeparator());
        distribution.context().forEach(line -> builder.append("  - ").append(line).append(System.lineSeparator()));
        builder.append(System.lineSeparator()).append("Artifacts:").append(System.lineSeparator());
        outcomes.stream().sorted(Comparator.comparing(outcome -> outcome.fileName)).forEach(outcome -> {
            builder.append("- ").append(outcome.fileName).append(" [").append(outcome.status).append("]")
                .append(System.lineSeparator())
                .append("    exactBytes=").append(outcome.exactBytes)
                .append(", classInventory=").append(outcome.classInventory)
                .append(", classMetadata=").append(outcome.classMetadata)
                .append(", manifest=").append(outcome.manifest)
                .append(", resources=").append(outcome.resources)
                .append(", linkage=").append(outcome.linkage)
                .append(", probe=").append(outcome.probe)
                .append(System.lineSeparator());
            if (outcome.linkage != null) {
                builder.append("    linkageCheckedClasses=").append(outcome.linkageCheckedClasses)
                    .append(", linkageExcludedClasses=").append(outcome.linkageExcludedClasses)
                    .append(System.lineSeparator());
            }
            if (!outcome.linkageExclusions.isEmpty()) {
                builder.append("    linkageExclusions:").append(System.lineSeparator());
                outcome.linkageExclusions.forEach(line -> builder.append("      - ").append(line).append(System.lineSeparator()));
            }
        });
        builder.append(System.lineSeparator()).append("JSON report: ").append(jsonReport.toAbsolutePath()).append(System.lineSeparator());
        builder.append("Text report: ").append(textReport.toAbsolutePath()).append(System.lineSeparator());
        if (!failures.isEmpty()) {
            builder.append(System.lineSeparator()).append("Failures:").append(System.lineSeparator());
            for (FailureDetail failure : failures) {
                builder.append("=== ").append(failure.artifact()).append(" / ").append(failure.check()).append(" ===")
                    .append(System.lineSeparator())
                    .append(failure.message()).append(System.lineSeparator());
                failure.context().forEach(line -> builder.append("  - ").append(line).append(System.lineSeparator()));
                if (failure.stackTrace() != null && !failure.stackTrace().isBlank()) {
                    builder.append(failure.stackTrace()).append(System.lineSeparator());
                }
            }
        }
        return builder.toString();
    }

    private static String failureToJson(FailureDetail failure) {
        return "    {\n"
            + "      \"artifact\": " + quote(failure.artifact()) + ",\n"
            + "      \"check\": " + quote(failure.check()) + ",\n"
            + "      \"message\": " + quote(failure.message()) + ",\n"
            + "      \"context\": " + jsonArray(failure.context()) + ",\n"
            + "      \"stackTrace\": " + (failure.stackTrace() == null ? "null" : quote(failure.stackTrace())) + "\n"
            + "    }";
    }

    private static String jsonArray(Collection<String> values) {
        return values.stream().map(ArtifactVerifier::quote).collect(Collectors.joining(", ", "[", "]"));
    }

    private static String limited(Collection<String> values) {
        List<String> items = values.stream().limit(12).toList();
        return items.stream().collect(Collectors.joining(", ", "[", values.size() > 12 ? ", ...]" : "]"));
    }

    private static String safe(Object value) {
        return value == null ? "<null>" : String.valueOf(value);
    }

    private static String joinList(List<?> values) {
        if (values == null || values.isEmpty()) {
            return "[]";
        }
        return values.stream().map(String::valueOf).collect(Collectors.joining(",", "[", "]"));
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"")
            .replace("\n", "\\n").replace("\r", "\\r") + "\"";
    }

    private static String normalize(Path path) {
        return path.toString().replace('\\', '/');
    }

    private static boolean isSignatureArtifact(String name) {
        String upper = name.toUpperCase(Locale.ROOT);
        return upper.startsWith("META-INF/")
            && (upper.endsWith(".SF") || upper.endsWith(".RSA") || upper.endsWith(".DSA")
            || upper.endsWith(".EC") || upper.substring("META-INF/".length()).startsWith("SIG-"));
    }

    private static boolean isSignatureAttribute(String name) {
        String upper = name.toUpperCase(Locale.ROOT);
        return upper.equals("SIGNATURE-VERSION")
            || upper.equals("MAGIC")
            || upper.endsWith("-DIGEST")
            || upper.contains("-DIGEST-");
    }

    private static String toBinaryName(String internalName) {
        return internalName.replace('/', '.');
    }

    private static byte[] hash(byte[] bytes) throws Exception {
        return MessageDigest.getInstance("SHA-256").digest(bytes);
    }

    private static String hex(byte[] bytes) {
        StringBuilder value = new StringBuilder();
        for (byte item : bytes) {
            value.append(String.format("%02x", item));
        }
        return value.toString();
    }

    private static String rootMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getClass().getSimpleName() + ": " + current.getMessage();
    }

    private static String stackTrace(Throwable error) {
        StringWriter writer = new StringWriter();
        error.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

    private static Path textReportPath(Path jsonReport) {
        String fileName = jsonReport.getFileName().toString();
        String textName = fileName.endsWith(".json") ? fileName.substring(0, fileName.length() - 5) + ".txt" : fileName + ".txt";
        return jsonReport.resolveSibling(textName);
    }

    private static List<ExclusionRule> rules(String reason, String... prefixes) {
        return Arrays.stream(prefixes).map(prefix -> new ExclusionRule(prefix, reason)).toList();
    }

    @SafeVarargs
    private static List<ExclusionRule> combineRules(List<ExclusionRule>... groups) {
        return Arrays.stream(groups).flatMap(List::stream).toList();
    }

    private static List<InnerClassNode> sortedInnerClasses(List<InnerClassNode> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream()
            .filter(item -> !"java/lang/invoke/MethodHandles$Lookup".equals(item.name))
            .filter(item -> item.innerName != null)
            .filter(item -> !isImplementationLocalClass(item.name))
            .sorted(Comparator.comparing((InnerClassNode item) -> safe(item.name))
                .thenComparing(item -> safe(item.outerName))
                .thenComparing(item -> safe(item.innerName))
                .thenComparingInt(item -> item.access))
            .toList();
    }

    private static boolean isRelevantInnerEntry(String ownerName, InnerClassNode inner) {
        return Objects.equals(inner.name, ownerName)
            || inner.name != null && inner.name.startsWith(ownerName + "$")
            || Objects.equals(inner.outerName, ownerName)
            || inner.name != null && ownerName.startsWith(inner.name + "$");
    }

    private static List<FieldNode> sortedFields(List<FieldNode> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream()
            .sorted(Comparator.comparing((FieldNode item) -> item.name)
                .thenComparing(item -> item.desc)
                .thenComparing(item -> safe(item.signature))
                .thenComparingInt(item -> item.access))
            .toList();
    }

    private static List<MethodNode> sortedMethods(List<MethodNode> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream()
            .sorted(Comparator.comparing((MethodNode item) -> item.name)
                .thenComparing(item -> item.desc)
                .thenComparing(item -> safe(item.signature))
                .thenComparingInt(item -> item.access))
            .toList();
    }

    private static List<ModuleRequireNode> sortedModuleRequires(List<ModuleRequireNode> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream().sorted(Comparator.comparing(item -> item.module)).toList();
    }

    private static List<ModuleExportNode> sortedModuleExports(List<ModuleExportNode> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream().sorted(Comparator.comparing(item -> item.packaze)).toList();
    }

    private static List<ModuleOpenNode> sortedModuleOpens(List<ModuleOpenNode> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream().sorted(Comparator.comparing(item -> item.packaze)).toList();
    }

    private static List<ModuleProvideNode> sortedModuleProvides(List<ModuleProvideNode> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream().sorted(Comparator.comparing(item -> item.service)).toList();
    }

    private static List<String> sortedStrings(List<String> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream().sorted().toList();
    }

    private static boolean isImplementationLocalClass(String className) {
        String simpleName = className.substring(className.lastIndexOf('/') + 1);
        String[] parts = simpleName.split("\\$");
        for (int index = 1; index < parts.length; index++) {
            if (!parts[index].isEmpty() && Character.isDigit(parts[index].charAt(0))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isClassMetadataExcluded(String className, List<ExclusionRule> exclusions) {
        return isImplementationLocalClass(className)
            || className.startsWith("org/lwjgl/util/mapped/MappedObjectTransformer")
            || findExclusion(exclusions, className) != null;
    }

    private static boolean isPrivateMember(int access) {
        return (access & Opcodes.ACC_PRIVATE) != 0;
    }

    private static boolean isSyntheticMember(int access) {
        return (access & (Opcodes.ACC_SYNTHETIC | Opcodes.ACC_BRIDGE)) != 0;
    }

    private static boolean shouldCompareField(String name, int access) {
        return !isSyntheticMember(access) && !isPrivateMember(access) && !"$assertionsDisabled".equals(name);
    }

    private static boolean shouldCompareMethod(String name, int access) {
        return !isSyntheticMember(access)
            && !"<clinit>".equals(name)
            && !name.startsWith("access$")
            && (!isPrivateMember(access) || (access & Opcodes.ACC_NATIVE) != 0);
    }

    private record ArtifactSpec(String fileName, String status, List<String> runtimeDependencies, List<ExclusionRule> linkageExclusions) {
    }

    private record ExclusionRule(String prefix, String reason) {
    }

    private record ExactMatch(boolean matches, String expectedSha256, String actualSha256) {
    }

    private record MatchResult(boolean matches, List<String> context) {
        private static MatchResult success() {
            return new MatchResult(true, List.of());
        }
    }

    private record JarSnapshot(Map<String, String> classes, String manifest, Map<String, String> resources, Set<String> duplicateEntries) {
    }

    private record JarComparison(MatchResult duplicateEntries, MatchResult classInventory, MatchResult classMetadata,
                                 MatchResult manifest, MatchResult resources) {
    }

    private record DistributionComparison(boolean matches, int expectedCount, int actualCount, List<String> context) {
    }

    private record LinkageFailure(String className, Throwable error) {
    }

    private record LinkageResult(boolean matches, int checkedClasses, int excludedClasses,
                                 List<String> exclusionSummary, List<String> context, String stackTrace) {
    }

    private record FailureDetail(String artifact, String check, String message, List<String> context, String stackTrace) {
    }

    @FunctionalInterface
    private interface Probe {
        String run(ClassLoader loader) throws Exception;
    }

    private static final class ArtifactOutcome {
        private final String fileName;
        private final String status;
        private Boolean exactBytes;
        private Boolean classInventory;
        private Boolean classMetadata;
        private Boolean manifest;
        private Boolean resources;
        private Boolean linkage;
        private Boolean probe;
        private int linkageCheckedClasses;
        private int linkageExcludedClasses;
        private List<String> linkageExclusions = List.of();

        private ArtifactOutcome(String fileName, String status) {
            this.fileName = fileName;
            this.status = status;
        }

        private String toJson() {
            List<String> fields = new ArrayList<>();
            fields.add("      \"fileName\": " + quote(fileName));
            fields.add("      \"status\": " + quote(status));
            if (exactBytes != null) {
                fields.add("      \"exactBytes\": " + exactBytes);
            }
            if (classInventory != null) {
                fields.add("      \"classInventory\": " + classInventory);
            }
            if (classMetadata != null) {
                fields.add("      \"classMetadata\": " + classMetadata);
            }
            if (manifest != null) {
                fields.add("      \"manifest\": " + manifest);
            }
            if (resources != null) {
                fields.add("      \"resources\": " + resources);
            }
            if (linkage != null) {
                fields.add("      \"linkage\": " + linkage);
                fields.add("      \"linkageCheckedClasses\": " + linkageCheckedClasses);
                fields.add("      \"linkageExcludedClasses\": " + linkageExcludedClasses);
                fields.add("      \"linkageExclusions\": " + jsonArray(linkageExclusions));
            }
            if (probe != null) {
                fields.add("      \"probe\": " + probe);
            }
            return "    {\n" + String.join(",\n", fields) + "\n    }";
        }
    }
}
