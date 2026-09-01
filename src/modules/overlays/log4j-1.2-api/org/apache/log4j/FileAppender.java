package org.apache.log4j;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.Writer;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.helpers.QuietWriter;

public class FileAppender extends WriterAppender {
   protected boolean fileAppend = true;
   protected String fileName = null;
   protected boolean bufferedIO = false;
   protected int bufferSize = 8192;

   public FileAppender() {
   }

   public FileAppender(final Layout layout, final String filename) throws IOException {
      this(layout, filename, true);
   }

   public FileAppender(final Layout layout, final String filename, final boolean append) throws IOException {
      this.layout = layout;
      this.setFile(filename, append, false, this.bufferSize);
   }

   public FileAppender(final Layout layout, final String filename, final boolean append, final boolean bufferedIO, final int bufferSize) throws IOException {
      this.layout = layout;
      this.setFile(filename, append, bufferedIO, bufferSize);
   }

   @Override
   public void activateOptions() {
      if (this.fileName != null) {
         try {
            this.setFile(this.fileName, this.fileAppend, this.bufferedIO, this.bufferSize);
         } catch (IOException e) {
            this.errorHandler.error("setFile(" + this.fileName + "," + this.fileAppend + ") call failed.", e, 4);
         }
      } else {
         LogLog.warn("File option not set for appender [" + this.name + "].");
         LogLog.warn("Are you using FileAppender instead of ConsoleAppender?");
      }
   }

   protected void closeFile() {
      if (this.qw != null) {
         try {
            this.qw.close();
         } catch (IOException e) {
            if (e instanceof InterruptedIOException) {
               Thread.currentThread().interrupt();
            }

            LogLog.error("Could not close " + this.qw, e);
         }
      }
   }

   public boolean getAppend() {
      return this.fileAppend;
   }

   public boolean getBufferedIO() {
      return this.bufferedIO;
   }

   public int getBufferSize() {
      return this.bufferSize;
   }

   public String getFile() {
      return this.fileName;
   }

   @Override
   protected void reset() {
      this.closeFile();
      this.fileName = null;
      super.reset();
   }

   public void setAppend(final boolean flag) {
      this.fileAppend = flag;
   }

   public void setBufferedIO(final boolean bufferedIO) {
      this.bufferedIO = bufferedIO;
      if (bufferedIO) {
         this.immediateFlush = false;
      }
   }

   public void setBufferSize(final int bufferSize) {
      this.bufferSize = bufferSize;
   }

   public void setFile(final String file) {
      String val = file.trim();
      this.fileName = val;
   }

   public synchronized void setFile(String fileName, boolean append, boolean bufferedIO, int bufferSize) throws IOException {
      LogLog.debug("setFile called: " + fileName + ", " + append);
      if (bufferedIO) {
         this.setImmediateFlush(false);
      }

      this.reset();
      FileOutputStream ostream = null;

      try {
         ostream = new FileOutputStream(fileName, append);
      } catch (FileNotFoundException ex) {
         label29: {
            String parentName = new File(fileName).getParent();
            if (parentName != null) {
               File parentDir = new File(parentName);
               if (!parentDir.exists() && parentDir.mkdirs()) {
                  ostream = new FileOutputStream(fileName, append);
                  break label29;
               }

               throw ex;
            }

            throw ex;
         }
      }

      Writer fw = this.createWriter(ostream);
      if (bufferedIO) {
         fw = new BufferedWriter(fw, bufferSize);
      }

      this.setQWForFiles(fw);
      this.fileName = fileName;
      this.fileAppend = append;
      this.bufferedIO = bufferedIO;
      this.bufferSize = bufferSize;
      this.writeHeader();
      LogLog.debug("setFile ended");
   }

   protected void setQWForFiles(final Writer writer) {
      this.qw = new QuietWriter(writer, this.errorHandler);
   }
}
