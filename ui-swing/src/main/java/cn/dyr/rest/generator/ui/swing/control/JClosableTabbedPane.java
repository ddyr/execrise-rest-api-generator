package cn.dyr.rest.generator.ui.swing.control;

import cn.dyr.rest.generator.ui.swing.SwingUIApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolTip;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * 一个会显示关闭图标的标签容器对象
 *
 * @author DENG YURONG
 */
public class JClosableTabbedPane extends JTabbedPane implements MouseListener {
    private static Logger logger;
    private static final boolean PANEL_PREVIEW = false;
    private static SwingUIApplication application = SwingUIApplication.getInstance();

    static {
        logger = LoggerFactory.getLogger(JClosableTabbedPane.class);
    }

    private double scaleRatio = 0.3;
    private HashMap<String, Component> maps = new HashMap<String, Component>();
    private TabbedPaneEventListener listener;

    public JClosableTabbedPane() {
        super();
        addMouseListener(this);
    }

    public TabbedPaneEventListener getListener() {
        return listener;
    }

    public JClosableTabbedPane setListener(TabbedPaneEventListener listener) {
        this.listener = listener;
        return this;
    }

    @Override
    public void addTab(String title, Component component) {
        this.addTab(title, component, null);
    }

    public void addTab(String title, Component component, Icon extraIcon) {
        super.addTab(title, new CloseTabIcon(extraIcon), component);
    }

    @Override
    public void insertTab(String title, Icon icon, Component component, String tip, int index) {
        if (PANEL_PREVIEW) {
            tip = "tab" + component.hashCode();
            maps.put(tip, component);
        } else {
            super.insertTab(title, icon, component, tip, index);
        }

        if (listener != null) {
            this.listener.onTabAdd(component);
        }
    }

    public void removeTabAt(int index) {
        Component component = getComponentAt(index);

        if (component instanceof DataPanel) {
            boolean dirty = ((DataPanel) component).isDirty();

            if (dirty) {
                int result = JOptionPane.showConfirmDialog(
                        JClosableTabbedPane.this,
                        "是否需要保存本页的数据？",
                        "REST API Generator", JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                    Runnable saveRunnable = ((DataPanel) component).saveData();
                    application.runBackground(saveRunnable);
                }
            }
        }

        maps.remove("tab" + component.hashCode());
        super.removeTabAt(index);

        if (this.listener != null) {
            this.listener.onTabRemoved(component);
        }
    }

    public JToolTip createToolTip() {
        if (PANEL_PREVIEW) {
            return super.createToolTip();
        } else {
            ImageToolTip tooltip = new ImageToolTip();
            tooltip.setComponent(this);
            return tooltip;
        }
    }

    class ImageToolTip extends JToolTip {
        public Dimension getPreferredSize() {
            String tip = getTipText();
            Component component = maps.get(tip);
            if (component != null) {
                return new Dimension((int) (getScaleRatio() * component.getWidth()), (int) (getScaleRatio() * component.getHeight()));
            } else {
                return super.getPreferredSize();
            }
        }

        public void paintComponent(Graphics g) {
            String tip = getTipText();
            Component component = maps.get(tip);
            if (component instanceof JComponent) {
                JComponent jcomponent = (JComponent) component;
                Graphics2D g2d = (Graphics2D) g;
                AffineTransform at = g2d.getTransform();
                g2d.transform(AffineTransform.getScaleInstance(getScaleRatio(), getScaleRatio()));
                ArrayList<JComponent> dbcomponents = new ArrayList<JComponent>();
                updateDoubleBuffered(jcomponent, dbcomponents);
                jcomponent.paint(g);
                resetDoubleBuffered(dbcomponents);
                g2d.setTransform(at);
            }
        }

        private void updateDoubleBuffered(JComponent component, ArrayList<JComponent> dbcomponents) {
            if (component.isDoubleBuffered()) {
                dbcomponents.add(component);
                component.setDoubleBuffered(false);
            }
            for (int i = 0; i < component.getComponentCount(); i++) {
                Component c = component.getComponent(i);
                if (c instanceof JComponent) {
                    updateDoubleBuffered((JComponent) c, dbcomponents);
                }
            }
        }

        private void resetDoubleBuffered(ArrayList<JComponent> dbcomponents) {
            for (JComponent component : dbcomponents) {
                component.setDoubleBuffered(true);
            }
        }
    }

    public double getScaleRatio() {
        return scaleRatio;
    }

    public void setScaleRatio(double scaleRatio) {
        this.scaleRatio = scaleRatio;
    }

    public void mouseClicked(MouseEvent e) {
        int tabNumber = getUI().tabForCoordinate(this, e.getX(), e.getY());
        if (tabNumber < 0) {
            return;
        }
        Rectangle rect = ((CloseTabIcon) getIconAt(tabNumber)).getBounds();
        if (rect.contains(e.getX(), e.getY())) {
            //the tab is being closed
            this.removeTabAt(tabNumber);
        }
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }
}

/**
 * 表示关闭按钮的图标
 */
class CloseTabIcon implements Icon {
    private int x_pos;
    private int y_pos;
    private int width;
    private int height;
    private Icon fileIcon;

    public CloseTabIcon(Icon fileIcon) {
        this.fileIcon = fileIcon;
        width = 16;
        height = 16;
    }

    public void paintIcon(Component c, Graphics g, int x, int y) {
        this.x_pos = x;
        this.y_pos = y;
        Color col = g.getColor();
        g.setColor(Color.black);
        int y_p = y + 2;
        g.drawLine(x + 1, y_p, x + 12, y_p);
        g.drawLine(x + 1, y_p + 13, x + 12, y_p + 13);
        g.drawLine(x, y_p + 1, x, y_p + 12);
        g.drawLine(x + 13, y_p + 1, x + 13, y_p + 12);
        g.drawLine(x + 3, y_p + 3, x + 10, y_p + 10);
        g.drawLine(x + 3, y_p + 4, x + 9, y_p + 10);
        g.drawLine(x + 4, y_p + 3, x + 10, y_p + 9);
        g.drawLine(x + 10, y_p + 3, x + 3, y_p + 10);
        g.drawLine(x + 10, y_p + 4, x + 4, y_p + 10);
        g.drawLine(x + 9, y_p + 3, x + 3, y_p + 9);
        g.setColor(col);
        if (fileIcon != null) {
            fileIcon.paintIcon(c, g, x + width, y_p);
        }
    }

    public int getIconWidth() {
        return width + (fileIcon != null ? fileIcon.getIconWidth() : 0);
    }

    public int getIconHeight() {
        return height;
    }

    public Rectangle getBounds() {
        return new Rectangle(x_pos, y_pos, width, height);
    }
}