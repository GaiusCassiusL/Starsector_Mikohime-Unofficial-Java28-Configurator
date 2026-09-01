package net.java.games.util.plugins.test;

import java.awt.Color;
import java.awt.Component;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

class ClassRenderer implements ListCellRenderer {
   JLabel label = new JLabel();

   public Component getListCellRendererComponent(JList jList, Object obj, int param, boolean param3, boolean param4) {
      this.label.setText(((Class)obj).getName());
      this.label.setForeground(Color.BLACK);
      this.label.setBackground(Color.WHITE);
      return this.label;
   }
}
