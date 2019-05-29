package org.gsonformat.intellij.ui;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiFile;

import org.gsonformat.intellij.ConvertBridge;
import org.gsonformat.intellij.entity.ClassEntity;
import org.gsonformat.intellij.entity.FieldEntity;
import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.treetable.DefaultMutableTreeTableNode;
import org.jdesktop.swingx.ux.CheckTreeTableManager;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Date;

import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import cn.vearn.checktreetable.FiledTreeTableModel;

import static javax.swing.ListSelectionModel.SINGLE_SELECTION;

public class FieldsDialog extends JFrame {

    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JPanel filedPanel;
    private JScrollPane sp;
    private PsiClass psiClass;
    private ClassEntity classEntity;
    private ConvertBridge.Operator operator;
    private PsiElementFactory factory;
    private PsiClass aClass;
    private PsiFile file;
    private Project project;
    private JLabel generateClass;
    private String generateClassStr;
    private ArrayList<DefaultMutableTreeTableNode> defaultMutableTreeTableNodeList;


    public FieldsDialog(ConvertBridge.Operator operator, ClassEntity classEntity,
                        PsiElementFactory factory, PsiClass psiClass, PsiClass aClass, PsiFile file, Project project
            , String generateClassStr) {
        this.operator = operator;
        this.factory = factory;
        this.aClass = aClass;
        this.file = file;
        this.project = project;
        this.psiClass = psiClass;
        this.generateClassStr = generateClassStr;
        setContentPane(contentPane);
        setTitle("配置字段名称及类型");
        getRootPane().setDefaultButton(buttonOK);
        this.setAlwaysOnTop(true);
        initListener(classEntity, generateClassStr);
    }

    private void initListener(ClassEntity classEntity, String generateClassStr) {
        this.classEntity = classEntity;
        defaultMutableTreeTableNodeList = new ArrayList<DefaultMutableTreeTableNode>();
        JXTreeTable treetable = new JXTreeTable(new FiledTreeTableModel(createData(classEntity)));
        CheckTreeTableManager manager = new CheckTreeTableManager(treetable);
        manager.getSelectionModel().addPathsByNodes(defaultMutableTreeTableNodeList);
        treetable.getColumnModel().getColumn(0).setPreferredWidth(150);
//        treetable.setSelectionBackground(treetable.getBackground());
        treetable.expandAll();
        treetable.setCellSelectionEnabled(false);
        final DefaultListSelectionModel defaultListSelectionModel = new DefaultListSelectionModel();
        treetable.setSelectionModel(defaultListSelectionModel);

        defaultListSelectionModel.setSelectionMode(SINGLE_SELECTION);
        defaultListSelectionModel.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                defaultListSelectionModel.clearSelection();
            }
        });
        defaultMutableTreeTableNodeList = null;
        treetable.setRowHeight(30);
        sp.setViewportView(treetable);
        generateClass.setText(generateClassStr);
        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });
        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    /**
     * 创建内容结果
     *
     * @param doubleShift 两个缩进
     * @param shift 单个缩进
     *
     * @param entity class信息,包含成员域和内部类信息
     */
    private StringBuilder createContent(ClassEntity entity,String shift,String doubleShift) {
        StringBuilder content = new StringBuilder();

        //class 头部
        content.append("class ")
                .append(entity.getClassName())
                .append(" with JsonHolderImpl<")
                .append(entity.getClassName())
                .append("> {\n");
        for (FieldEntity fieldEntity : entity.getFields()) {
            //get  方法
            content.append(shift)
                    .append("/// [key : value] => [")
                    .append(fieldEntity.getKey())
                    .append(" : ")
                    .append(fieldEntity.getValue())
                    .append("]\n")
                    .append(shift)
                    .append(fieldEntity.getBriefType())
                    .append(" get ")
                    .append(fieldEntity.getFieldName())
                    .append(" => getValue(\"")
                    .append(fieldEntity.getFieldName())
                    .append("\");\n");

            //set方法
            content.append(shift)
                    .append("set ")
                    .append(fieldEntity.getFieldName())
                    .append("(")
                    .append(fieldEntity.getBriefType())
                    .append(" value) => setValue(\"")
                    .append(fieldEntity.getFieldName())
                    .append("\", value);\n\n");
        }

        // fromJson 构造函数 (内部需初始化其他bean)
        StringBuilder childInit = new StringBuilder();
        for (ClassEntity childClass : entity.getInnerClasss()) {
            childInit.append(doubleShift)
                    .append(childClass.getClassName())
                    .append(".fromJson();\n");
        }
        content.append(shift)
                .append(entity.getClassName())
                .append(".fromJson([Map<String, dynamic> json]) {\n")
                .append(doubleShift)
                .append("fromJson(json);\n")
                .append(childInit)
                .append(shift)
                .append("}\n\n");

        // provideCreator 方法
        content.append(shift)
                .append("@override\n")
                .append(shift)
                .append("JsonHolderImpl<")
                .append(entity.getClassName())
                .append("> provideCreator(Map<String, dynamic> json) {\n")
                .append(doubleShift)
                .append("return ")
                .append(entity.getClassName())
                .append(".fromJson(json);\n")
                .append(shift)
                .append("}\n\n");

        // provideListCreator 方法
        content.append(shift)
                .append("@override\n")
                .append(shift)
                .append("List<")
                .append(entity.getClassName())
                .append("> provideListCreator() {\n")
                .append(doubleShift)
                .append("return <")
                .append(entity.getClassName())
                .append(">[];\n")
                .append(shift)
                .append("}\n\n");

        // provideListListCreator 方法
        content.append(shift)
                .append("@override\n")
                .append(shift)
                .append("List<List<")
                .append(entity.getClassName())
                .append(">> provideListListCreator() {\n")
                .append(doubleShift)
                .append("return <List<")
                .append(entity.getClassName())
                .append(">>[];\n")
                .append(shift)
                .append("}\n\n");

        //class 尾部
        content.append("}\n\n");

        //添加其他class类
        for (ClassEntity childClass : entity.getInnerClasss()) {
            content.append(createContent(childClass,shift , doubleShift));
        }

        return content;
    }

    private void onOK() {
        this.setAlwaysOnTop(false);

        StringBuilder result = new StringBuilder();

        //导入引用类(TODO)
        String currentTime = new Date().toString();
        result.append("//******************************************************************\n")
                .append("//**************************** Generate By JsonToDartBean  **********\n")
                .append("//**************************** ")
                .append(currentTime)
                .append("  **********\n")
                .append("//******************************************************************\n\n")
                .append("import 'package:json_holder_impl/json_holder_impl.dart';\n\n")
                .append(createContent(classEntity,"  ","    "));


        // TODO 在dialog中显示结果
        ResultDialog resultDialog = new ResultDialog(null, null, project, result.toString());
        resultDialog.setSize(800, 500);
        resultDialog.setLocationRelativeTo(null);
        resultDialog.setVisible(true);
        dispose();
        operator.dispose();

        /*WriteCommandAction.runWriteCommandAction(project, new Runnable() {

            @Override
            public void run() {
                if (psiClass == null) {
                    try {
                        psiClass = PsiClassUtil.getPsiClass(file, project, generateClassStr);
                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                        operator.showError(ConvertBridge.Error.DATA_ERROR);
                        Writer writer = new StringWriter();
                        PrintWriter printWriter = new PrintWriter(writer);
                        throwable.printStackTrace(printWriter);
                        printWriter.close();
                        operator.setErrorInfo(writer.toString());
                        operator.setVisible(true);
                        operator.showError(ConvertBridge.Error.PATH_ERROR);
                    }
                }

                if (psiClass != null) {
                    String[] arg = generateClassStr.split("\\.");
                    if (arg.length > 1) {
                        Config.getInstant().setEntityPackName(generateClassStr.substring(0, generateClassStr.length() - arg[arg.length - 1].length()));
                        Config.getInstant().save();
                    }
                    try {
                        setVisible(false);
                        DataWriter dataWriter = new DataWriter(file, project, psiClass);
                        dataWriter.execute(classEntity);
                        Config.getInstant().saveCurrentPackPath(StringUtils.getPackage(generateClassStr));
                        operator.dispose();
                        dispose();
                    } catch (Exception e) {
                        e.printStackTrace();
                        operator.showError(ConvertBridge.Error.PARSE_ERROR);
                        Writer writer = new StringWriter();
                        PrintWriter printWriter = new PrintWriter(writer);
                        e.printStackTrace(printWriter);
                        printWriter.close();
                        operator.setErrorInfo(writer.toString());
                        operator.setVisible(true);
                        dispose();
                    }
                }
            }
        });*/
    }

    private void onCancel() {
        operator.setVisible(true);
        dispose();
    }

    private DefaultMutableTreeTableNode createData(ClassEntity classEntity) {
        DefaultMutableTreeTableNode root = new DefaultMutableTreeTableNode(classEntity);
        createDataNode(root, classEntity);
        return root;
    }

    private void createDataNode(DefaultMutableTreeTableNode root, ClassEntity innerClassEntity) {
        for (FieldEntity field : innerClassEntity.getFields()) {
            DefaultMutableTreeTableNode node = new DefaultMutableTreeTableNode(field);
            root.add(node);
            defaultMutableTreeTableNodeList.add(node);
        }
        for (ClassEntity classEntity : innerClassEntity.getInnerClasss()) {
            DefaultMutableTreeTableNode node = new DefaultMutableTreeTableNode(classEntity);
            root.add(node);
            createDataNode(node, classEntity);
        }

    }

}
