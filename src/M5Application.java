import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import ArgumentProcessors.AccessLevelArgumentProcessor;
import ArgumentProcessors.BasicCommandLineProcessor;
import ArgumentProcessors.OutputFileCommandLineArgumentProcessorDecorator;
import ArgumentProcessors.PatternDetectorArgumentProcessor;
import ArgumentProcessors.RecursionArgumentProcessor;
import ArgumentProcessors.SettingsFileCommandArgumentProcessor;
import GraphBuilding.AssociationDependencyChecker;
import GraphBuilding.AssociationEdgeGenerator;
import GraphBuilding.BidirectionalEdgeChecker;
import GraphBuilding.CodeDependencyEdgeGenerator;
import GraphBuilding.DependencyEdgeGenerator;
import GraphBuilding.DuplicateDependencyEdgeChecker;
import GraphBuilding.ExtendsEdgeGenerator;
import GraphBuilding.ImplementsEdgeGenerator;
import GraphReading.AssociationBidirectionalEdgeReader;
import GraphReading.AssociationEdgeReader;
import GraphReading.DependencyBidirectionalEdgeReader;
import GraphReading.DependencyEdgeReader;
import GraphReading.ExtendsEdgeReader;
import GraphReading.GraphVizEdgeReader;
import GraphReading.GraphVizNodeReader;
import GraphReading.ImplementsEdgeReader;
import GraphReading.NormalNodeReader;
import application.CodeProcessor;

public class M5Application {
	public static void main(String[] args) throws IOException {
		BasicCommandLineProcessor c = new BasicCommandLineProcessor();
		OutputFileCommandLineArgumentProcessorDecorator o = new OutputFileCommandLineArgumentProcessorDecorator(c);
		RecursionArgumentProcessor r = new RecursionArgumentProcessor(o);
		AccessLevelArgumentProcessor a = new AccessLevelArgumentProcessor(r);
		PatternDetectorArgumentProcessor p = new PatternDetectorArgumentProcessor(a);
		SettingsFileCommandArgumentProcessor s = new SettingsFileCommandArgumentProcessor(p);


		//		tmp.addGraphMutator(new SameSimpleNameMutator(tmp.getFieldReaders(), tmp.getMethodReaders()));
		//		tmp.addGraphMutator(new LawOfDemeterMutator());


		JFrame frame = new JFrame();
		JPanel panel = new JPanel();
		JButton button = new JButton();
		JTextField settingFile = new JTextField();
		settingFile.setPreferredSize(new Dimension(120, 30));
		settingFile.setText("./default.cfg");

		JTextField outputFile = new JTextField();
		outputFile.setPreferredSize(new Dimension(120, 30));
		outputFile.setText("./input_output/output.png");

		JCheckBox recursive = new JCheckBox("recursive node generation");

		JCheckBox useConfig = new JCheckBox("use config file");
		useConfig.setSelected(true);


		ButtonGroup privacy = new ButtonGroup();
		JRadioButton priv = new JRadioButton("private");
		JRadioButton prot = new JRadioButton("protected");
		JRadioButton pub = new JRadioButton("public");

		JPanel privacyPanel = new JPanel();
		privacyPanel.setLayout(new BoxLayout(privacyPanel, BoxLayout.Y_AXIS));

		JTextArea whitelist = new JTextArea();
		whitelist.setPreferredSize(new Dimension(200, 200));
		whitelist.setText("whitelist");

		JTextArea blacklist = new JTextArea();
		blacklist.setPreferredSize(new Dimension(200, 200));
		blacklist.setText("blacklist");


		JTextArea patterns = new JTextArea();
		patterns.setPreferredSize(new Dimension(200, 200));
		patterns.setText("patterns");

		privacy.add(priv);
		privacy.add(prot);
		privacy.add(pub);

		privacyPanel.add(priv);
		privacyPanel.add(prot);
		privacyPanel.add(pub);

		button.setText("run");
		button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					CodeProcessor tmp;


					if(useConfig.isSelected()) {
						String[] arg = new String[] {"cfg=" + settingFile.getText()};
						tmp = s.process(arg);
					} else {
						String access = "private";

						if (prot.isSelected()) {
							access = "protected";
						}

						if (pub.isSelected()) {
							access = "public";
						}


						ArrayList<String> arg = new ArrayList<String>();

						arg.add("of=" + outputFile.getText());
						arg.add("whitelist=" + whitelist.getText());
						arg.add("recursive=" + recursive.isSelected());
						if(!blacklist.getText().equals("blacklist") &&
								!blacklist.getText().equals("") &&
								recursive.isSelected()){
							arg.add("blacklist=" + blacklist.getText());
						}
						arg.add("access=" + access);
						if(!patterns.getText().equals("patterns") &&
								!patterns.getText().equals("")){
							arg.add("patterns=" + patterns.getText());
						}


						tmp = p.process(arg.toArray(new String[0]));
					}

					tmp.addEdgeGenerator(new ExtendsEdgeGenerator());
					tmp.addEdgeGenerator(new ImplementsEdgeGenerator());
					tmp.addEdgeGenerator(new AssociationEdgeGenerator());
					tmp.addEdgeGenerator(new DependencyEdgeGenerator());
					tmp.addEdgeGenerator(new CodeDependencyEdgeGenerator());

					tmp.addEdgeReader(new ExtendsEdgeReader());
					tmp.addEdgeReader(new ImplementsEdgeReader());
					tmp.addEdgeReader(new AssociationEdgeReader());
					tmp.addEdgeReader(new DependencyEdgeReader());
					tmp.addEdgeReader(new AssociationBidirectionalEdgeReader());
					tmp.addEdgeReader(new DependencyBidirectionalEdgeReader());
					tmp.addEdgeReader(new GraphVizEdgeReader());

					tmp.addNodeReader(new NormalNodeReader());
					tmp.addNodeReader(new GraphVizNodeReader());

					tmp.addEdgeChecker(new AssociationDependencyChecker());
					tmp.addEdgeChecker(new BidirectionalEdgeChecker());
					tmp.addEdgeChecker(new DuplicateDependencyEdgeChecker());

					tmp.process();

					Thread.sleep(200);

					JFrame imageFrame = new JFrame("image");
					JPanel imgPanel = new JPanel();
					JLabel imgLabel = new JLabel();
					ImageIcon img = new ImageIcon(outputFile.getText());

					imgLabel.setIcon(img);
					imgPanel.add(imgLabel);
					imageFrame.add(imgPanel);

					imageFrame.setVisible(true);

				} catch (IOException | InterruptedException e1) {
					throw new RuntimeException(e1);
				}

			}
		});


		frame.setTitle("SwagLagUMLGenerator");
		frame.add(panel);

		panel.add(privacyPanel);

		panel.add(outputFile);
		panel.add(recursive);
		panel.add(useConfig);
		panel.add(settingFile);

		panel.add(whitelist);
		panel.add(blacklist);
		panel.add(patterns);

		frame.setBounds(0, 0, 600, 800);

		panel.add(button);
		frame.setVisible(true);

	}

}
