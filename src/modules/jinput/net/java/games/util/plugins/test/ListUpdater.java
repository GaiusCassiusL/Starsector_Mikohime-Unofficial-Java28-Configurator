package net.java.games.util.plugins.test;

import javax.swing.DefaultListModel;
import javax.swing.JList;

class ListUpdater implements Runnable {
   Object[] objList;
   DefaultListModel mdl;

   public ListUpdater(JList jlist, Object[] list) {
      this.objList = list;
      this.mdl = (DefaultListModel)jlist.getModel();
   }

   public void run() {
      this.mdl.clear();

      for (int i = 0; i < this.objList.length; i++) {
         this.mdl.addElement(this.objList[i]);
      }
   }
}
