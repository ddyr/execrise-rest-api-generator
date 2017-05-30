package cn.dyr.rest.generator.ui.swing.panel;

import cn.dyr.rest.generator.ui.swing.SwingUIApplication;
import cn.dyr.rest.generator.ui.swing.context.ProjectGenerator;
import cn.dyr.rest.generator.ui.swing.model.UUIDIdentifier;
import cn.dyr.rest.generator.ui.swing.util.MessageBoxUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.Border;
import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.InputStream;
import java.util.UUID;

/**
 * 用于指定代码生成参数的页面
 *
 * @author DENG YURONG
 * @version 0.1.0001
 */
public class GenerationPanel extends JPanel implements ActionListener, UUIDIdentifier {

    private static Logger logger;

    static {
        logger = LoggerFactory.getLogger(GenerationPanel.class);
    }

    private String id;

    // <editor-fold desc="界面相关代码">

    private JTextField targetFileURL;
    private JFileChooser fileChooser;

    private JTextArea logArea;

    private JCheckBox generateExecutableFile;

    private JPanel outputPanel;
    private JPanel logPanel;

    private JButton generateButton;
    private JButton browserButton;

    private void initComponents() {
        this.fileChooser = new JFileChooser();
        this.fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        this.outputPanel = new JPanel(new GridLayout(3, 1));
        this.outputPanel.setBorder(BorderFactory.createTitledBorder("代码输出信息"));

        this.generateButton = new JButton("生成");
        this.generateButton.addActionListener(this);

        this.browserButton = new JButton("浏览");
        this.browserButton.addActionListener(this);

        this.targetFileURL = new JTextField();
        this.targetFileURL.setEditable(false);

        this.generateExecutableFile = new JCheckBox("生成可执行文件");

        this.logArea = new JTextArea();
        this.logArea.setEditable(false);

        this.logPanel = new JPanel(new BorderLayout());

        // 代码输出的面板
        // 面板的第一行
        JLabel label = new JLabel("请选择输出的目标目录：");
        JPanel firstRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        firstRow.add(label);
        outputPanel.add(firstRow);

        // 第二行
        JPanel secondRow = new JPanel(new BorderLayout());
        secondRow.add(this.targetFileURL, BorderLayout.CENTER);
        secondRow.add(this.browserButton, BorderLayout.EAST);
        outputPanel.add(secondRow);

        // 第三行
        JPanel lastRow = new JPanel(new BorderLayout());
        lastRow.add(this.generateButton, BorderLayout.EAST);
        lastRow.add(this.generateExecutableFile, BorderLayout.WEST);
        outputPanel.add(lastRow);

        // 日志输出面板
        this.logPanel.setBorder(BorderFactory.createTitledBorder("生成日志"));
        this.logPanel.add(new JScrollPane(this.logArea), BorderLayout.CENTER);

        // 用于组织两个面板的临时面板
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(this.outputPanel, BorderLayout.NORTH);
        mainPanel.add(this.logPanel, BorderLayout.CENTER);

        this.add(mainPanel, BorderLayout.CENTER);
    }

    // </editor-fold>

    public GenerationPanel() {
        super(new BorderLayout());

        this.initComponents();

        this.id = UUID.randomUUID().toString();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        if (source == this.browserButton) {
            int result = this.fileChooser.showSaveDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                String path = this.fileChooser.getSelectedFile().getAbsolutePath();

                this.targetFileURL.setText(path);
            }

        } else if (source == this.generateButton) {
            Thread thread = new Thread(new GenerateThread());
            thread.start();
        }
    }

    private void setInputEnabled(final boolean enabled) {
        EventQueue.invokeLater(() -> {
            browserButton.setEnabled(enabled);
            generateButton.setEnabled(enabled);
            generateExecutableFile.setEnabled(enabled);
        });
    }

    @Override
    public String getId() {
        return this.id;
    }

    private class GenerateThread implements Runnable {

        private boolean executableFileGenerated;

        GenerateThread() {
            this.executableFileGenerated = generateExecutableFile.isSelected();
        }

        @Override
        public void run() {
            String targetFileURLText = targetFileURL.getText();
            if (targetFileURLText == null || "".equals(targetFileURLText.trim())) {
                EventQueue.invokeLater(() -> JOptionPane.showMessageDialog(GenerationPanel.this, "请输入有效的路径", "REST API Generator", JOptionPane.ERROR_MESSAGE));
                return;
            }

            File targetDir = new File(targetFileURLText);
            if (targetDir.exists() && !targetDir.isDirectory()) {
                EventQueue.invokeLater(() -> JOptionPane.showMessageDialog(
                        GenerationPanel.this, "该路径不能用作输出路径，请重新选择", "REST API Generator", JOptionPane.ERROR_MESSAGE));
                return;
            }

            try {
                setInputEnabled(false);

                // 生成代码文件
                ProjectGenerator generator = new ProjectGenerator(SwingUIApplication.getInstance().getCurrentProjectContext());
                generator.generate(targetDir);

                // 对代码进行打包
                if (executableFileGenerated) {
                    String pathValue = System.getenv("Path");
                    String javaHomeValue = System.getenv("JAVA_HOME");
                    String classpath = System.getenv("CLASSPATH");

                    Runtime currentRunTime = Runtime.getRuntime();
                    Process process = currentRunTime.exec("cmd /c mvn package", new String[]{
                            String.format("Path=%s", pathValue),
                            String.format("JAVA_HOME=%s", javaHomeValue),
                            String.format("CLASSPATH=%s", classpath)
                    }, targetDir);
                    int code = process.waitFor();

                    logger.info("mvn return code: {}", code);

                    InputStream errorStream = process.getErrorStream();
                    String content = IOUtils.toString(errorStream, "GBK");

                    logger.info("stderr:");
                    logger.info("{}", content);
                }

                EventQueue.invokeLater(() -> JOptionPane.showMessageDialog(GenerationPanel.this, "代码生成成功！"));
            } catch (Exception e) {
                logger.error("exception occurred during generating code...", e);
                MessageBoxUtils.showExceptionMessageBox(GenerationPanel.this, e);
            } finally {
                setInputEnabled(true);
            }
        }
    }
}
