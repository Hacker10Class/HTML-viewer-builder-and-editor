package dialogs;

import static com.mongodb.client.model.Filters.eq;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.RootPaneContainer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Element;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import engine.FileSaver;
import engine.HTMLDocReader;
import engine.Main;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import listeners.newElementFieldDocumentListener;

public class EditElementDialog {

	static Element element;
	HTMLDocReader reader;
	static String fullHTML;
	
	private JTabbedPane tabbedPane;
	private static JTextArea textArea;
	private final JFXPanel fxPanel = new JFXPanel();
	private static WebView webView;
	private static WebEngine webEngine;
	private String tempElementURL;
	private static File tempFile;
	private static JSplitPane previewPane;
	public static ArrayList<JLabel> label;
	public static ArrayList<JTextField> field;
	
	private String[] tabTitles = { "Global Attributes", "Element Attributes", "Style",
			"Events" };
	private JPanel[] tabPanels = new JPanel[tabTitles.length];

	private String[] globalHTMLAttributes = { "id", "accesskey", "contenteditable", "contextmenu", "dir", "draggable",
			"dropzone", "lang", "spellcheck", "tabindex", "title", "translate" };

	public EditElementDialog(Element element, HTMLDocReader reader) {
		this.element = element;
		//remove highlighter css class from element
		element.removeClass("java-highlighted-element");
		element.removeClass(".java-highlighted-element");
		if (element.className().equals("")) {
			element.removeAttr("class");
		}
		this.reader = reader;
		fullHTML = element.outerHtml();
		final JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());

		final JDialog dialog = new JDialog(Main.frame, "Edit Element", true);
		RootPaneContainer root = (RootPaneContainer) Main.frame.getRootPane().getTopLevelAncestor();
		root.getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		root.getGlassPane().setVisible(true);

		MongoClient mongoClient = new MongoClient(
				new MongoClientURI("mongodb://user:password@ds151024.mlab.com:51024/htmlelements"));
		MongoDatabase db = mongoClient.getDatabase("htmlelements");
		MongoCollection<Document> elementsCollection = db.getCollection("elements");
		String html = "<"+element.tagName()+">";
		Document doc = elementsCollection.find(eq("name", html)).first();
		Dimension d = new Dimension(150, 20);
		// create all tab panels
		for (int i = 0; i < tabTitles.length; i++) {
			tabPanels[i] = new JPanel();
			tabPanels[i].setLayout(new BoxLayout(tabPanels[i], BoxLayout.PAGE_AXIS));
			tabPanels[i].setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
			tabPanels[i].add(Box.createHorizontalGlue());
			tabPanels[i].add(Box.createRigidArea(new Dimension(5, 5)));
			tabPanels[i].revalidate();

		}

		// tab 1

		JLabel l = new JLabel("Global HTML Attributes");
		l.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
		l.setFont(new Font("Arial", Font.BOLD, 15));
		tabPanels[0].add(l);

		label = new ArrayList<JLabel>();
		field = new ArrayList<JTextField>();
		for (int i = 0; i < globalHTMLAttributes.length; i++) {
			label.add(new JLabel(globalHTMLAttributes[i]));
			if (element.hasAttr(globalHTMLAttributes[i])) {
				field.add(new JTextField(element.attr(globalHTMLAttributes[i])));
			} else {
				field.add(new JTextField(""));

			}
		}
		// p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		for (int i = 0; i < label.size(); i++) {

			// b.setLayout(new FlowLayout(FlowLayout.LEADING));

			tabPanels[0].add(label.get(i));
			tabPanels[0].add(field.get(i));
			label.get(i).setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
			label.get(i).setAlignmentX(Component.LEFT_ALIGNMENT);
			field.get(i).setAlignmentX(Component.LEFT_ALIGNMENT);
			field.get(i).setPreferredSize(d);
			field.get(i).setHorizontalAlignment(JTextField.LEFT);
		}
		tabPanels[0].revalidate();
		JCheckBox hiddenCheck = new JCheckBox("hidden");
		if(element.hasAttr("hidden")) {
			hiddenCheck.setSelected(true);
		}
		hiddenCheck.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				// TODO Auto-generated method stub
				// el = getJsoupElement();

				System.out.println(hiddenCheck.isSelected());
				if (hiddenCheck.isSelected()) {
					element.attr(hiddenCheck.getText(), "");
				} else if (!hiddenCheck.isSelected()) {
					element.removeAttr(hiddenCheck.getText());
				}
				fullHTML = element.toString();
				updateDoc();

				// SET HIDDEN
			}
		});
		tabPanels[0].add(hiddenCheck);
		tabPanels[0].setAlignmentX(Component.LEFT_ALIGNMENT);

		// Tab 2
		JLabel esa = new JLabel("Element Specific Attributes");
		esa.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
		esa.setFont(new Font("Arial", Font.BOLD, 15));
		tabPanels[1].add(esa);

		try {
			JSONArray attrObj;
			JSONObject object = new JSONObject(doc.toJson());

			// Database may return weird tags such as <img></img>.
			// parsing to jsoup elements and then back to string will fix the tags to be
			// appropriate
			attrObj = object.getJSONArray("attributes");
			JPanel valuePanel = new JPanel();
			// valuePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
			valuePanel.setLayout(new BoxLayout(valuePanel, BoxLayout.PAGE_AXIS));
			valuePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			valuePanel.add(Box.createHorizontalGlue());
			valuePanel.add(Box.createRigidArea(new Dimension(5, 5)));
			for (int i = 0; i < attrObj.length(); i++) {
				JSONObject objectFromArray = attrObj.getJSONObject(i);
				// System.out.println(objectFromArray);
				Iterator<String> iterator = objectFromArray.keys();
				while (iterator.hasNext()) {
					// THIS IS THE KEY THAT IS USED TO MAKE THE ATTRIBUTE NAMES JLABEL

					JPanel optionsPanel = new JPanel();

					String key = iterator.next();
					JLabel keyLabel = new JLabel(key);
					keyLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
					optionsPanel.add(keyLabel);
					// KEY ARRAY
					JSONArray keyArray = objectFromArray.getJSONArray(key);

					optionsPanel.setBorder(BorderFactory.createLineBorder(Color.BLUE));
					optionsPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 20, 20));
					optionsPanel.setMaximumSize(new Dimension(500, 75));
					for (int j = 0; j < keyArray.length(); j++) {
						// System.out.println(keyArray.get(j));
						JSONObject lastObject = keyArray.getJSONObject(j);
						// DETERMINES WHAT TYPE OF COMPONENT TO CREATE BY READING THE OBJECT'S NAME
						String type = lastObject.names().get(0).toString();
						// System.out.println("Type equals: "+type+"for key "+key);
						if (type.equals("text")) {
							JTextField elementText = new JTextField();
							elementText.setPreferredSize(d);
							optionsPanel.add(elementText);
							elementText.setText(element.text());
							elementText.getDocument().addDocumentListener(new DocumentListener() {

								@Override
								public void changedUpdate(DocumentEvent e) {
									// TODO Auto-generated method stub
									updateElement();
								}

								@Override
								public void insertUpdate(DocumentEvent e) {
									// TODO Auto-generated method stub
									updateElement();
								}

								@Override
								public void removeUpdate(DocumentEvent e) {
									// TODO Auto-generated method stub
									updateElement();
								}

								public void updateElement() {
									element.text(elementText.getText());
									fullHTML = element.toString();
									updateDoc();
								}

							});

						}
						if (type.equals("value")) {
							JSONArray valueArray = lastObject.getJSONArray("value");
							List<String> valueList = new ArrayList<String>();
							valueList.add("");
							for (int k = 0; k < valueArray.length(); k++) {
								valueList.add(valueArray.get(k).toString());
							}
							String[] valueStringList = valueList.toArray(new String[valueList.size()]);
							if (valueStringList[1].equals("true")) {
								JCheckBox checkBox = new JCheckBox(key);
								checkBox.addItemListener(new ItemListener() {
									@Override
									public void itemStateChanged(ItemEvent e) {
										// TODO Auto-generated method stub
										// el = getJsoupElement();
										System.out.println(checkBox.isSelected());
										if (checkBox.isSelected()) {
											element.attr(checkBox.getText(), "");
										} else if (!checkBox.isSelected()) {
											element.removeAttr(checkBox.getText());
										}
										fullHTML = element.toString();
										updateDoc();
									}
								});
								optionsPanel.add(checkBox);
							} else {
								JComboBox<String> valueComboBox = new JComboBox<String>(valueStringList);
								valueComboBox.setMaximumSize(d);
								valueComboBox.setAlignmentX(Component.LEFT_ALIGNMENT);
								valueComboBox.addItemListener(new ItemListener() {
									@Override
									public void itemStateChanged(ItemEvent e) {
										// System.out.println(html);
										if (e.getItem().equals("")) {
											// System.out.println("removing");
											element.removeAttr(key);
										} else {
											element.attr(key, e.getItem().toString());
										}
										fullHTML = element.toString();
										updateDoc();
									}

								});
								optionsPanel.add(valueComboBox);
							}
						}

						if (type.equals("unit")) {
							JSONArray unitArray = lastObject.getJSONArray("unit");
							if (unitArray.length() == 1) {
								// System.out.println(unitArray.get(0));

								if (unitArray.getString(0).equals("pixels")) {
									// NumberFormat format = NumberFormat.getIntegerInstance();
									// NumberFormatter formatter = new NumberFormatter(format);
									// formatter.setValueClass(Integer.class);
									// formatter.setMinimum(0);
									// formatter.setMaximum(Integer.MAX_VALUE);
									// formatter.setAllowsInvalid(false);
									// // If you want the value to be committed on each keystroke instead of focus
									// lost
									// formatter.setCommitsOnValidEdit(true);
									//
									// JFormattedTextField numberTextField = new JFormattedTextField(formatter);
									JTextField numberTextField = new JTextField();
									numberTextField.setPreferredSize(d);
									numberTextField.getDocument().addDocumentListener(new DocumentListener() {

										@Override
										public void changedUpdate(DocumentEvent e) {
											// TODO Auto-generated method stub
											updateElement();
										}

										@Override
										public void insertUpdate(DocumentEvent e) {
											// TODO Auto-generated method stub
											updateElement();
										}

										@Override
										public void removeUpdate(DocumentEvent e) {
											// TODO Auto-generated method stub
											System.out.println(numberTextField);
											updateElement();
										}

										public void updateElement() {
											if (numberTextField.getText().equals("")
													|| numberTextField.getText() == null) {
												element.removeAttr(key);
											} else {
												element.attr(key, numberTextField.getText());
											}
											fullHTML = element.toString();
											updateDoc();
										}

									});
									optionsPanel.add(numberTextField);
								} else {
									JTextField textField = new JTextField();
									textField.setPreferredSize(d);
									textField.getDocument().addDocumentListener(new DocumentListener() {

										@Override
										public void changedUpdate(DocumentEvent e) {
											// TODO Auto-generated method stub
											updateElement();
										}

										@Override
										public void insertUpdate(DocumentEvent e) {
											// TODO Auto-generated method stub
											updateElement();
										}

										@Override
										public void removeUpdate(DocumentEvent e) {
											// TODO Auto-generated method stub
											updateElement();
										}

										public void updateElement() {
											if (textField.getText().equals("") || textField.getText() == null) {
												element.removeAttr(key);
											} else {
												element.attr(key, textField.getText());
											}
											fullHTML = element.toString();
											updateDoc();
										}

									});
									optionsPanel.add(textField);
								}

								JLabel unitLabel = new JLabel(" - " + unitArray.getString(0));
								Font font = new Font("Arial", 2, 12);
								unitLabel.setFont(font);
								optionsPanel.add(unitLabel);
							}
						}

						if (type.equals("action")) {
							JSONArray actionArray = lastObject.getJSONArray("action");
							if (actionArray.get(0).toString().equals("upload")) {
								JButton uploadButton = new JButton("Upload");
								optionsPanel.add(uploadButton);
							}
						}
					}
					valuePanel.add(optionsPanel);
				}
			}

			tabPanels[1].add(valuePanel);
		} catch (Exception e1) {
			tabPanels[1].add(new JLabel("Element does not have attributes that are specific to it."));
		}

		// try {
		// fullHTML = doc.getString("tag");
		// } catch (Exception e) {
		// // TODO Auto-generated catch block
		// fullHTML = html;
		// }

		try {
			tempFile = File.createTempFile("HTMLEditAttributeTemp", ".html");
			BufferedWriter bw = new BufferedWriter(new FileWriter(tempFile));
			bw.write(fullHTML);
			bw.close();
			tempFile.deleteOnExit();
			tempElementURL = tempFile.getAbsolutePath();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		textArea = new JTextArea(fullHTML);
		textArea.setFont(new Font("Arial", Font.BOLD, 15));
		tabbedPane = new JTabbedPane();
		previewPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, fxPanel, textArea);
		previewPane.setDividerLocation((Toolkit.getDefaultToolkit().getScreenSize().height) / 6);
		JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, previewPane, tabbedPane);

		// creat the tabs
		for (int i = 0; i < tabTitles.length; i++) {
			JScrollPane scrollPane = new JScrollPane(tabPanels[i]);
			scrollPane.getVerticalScrollBar().setUnitIncrement(10);
			JComponent c = scrollPane;
			tabbedPane.addTab(tabTitles[i], null, c, tabTitles[i]);
		}

		// tab 3 content

		JPanel bottomPanel = new JPanel();
		FlowLayout layout = new FlowLayout(FlowLayout.TRAILING, 20, 20);
		bottomPanel.setLayout(layout);
		JButton confirmButton = new JButton("Confirm");
		JButton cancelButton = new JButton("Cancel");
		bottomPanel.add(confirmButton);
		bottomPanel.add(cancelButton);

		confirmButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				System.out.println("Confirm Button pressed");
				try {
					reader.updateTempDoc();
					Main.updateFrame();
					mongoClient.close();
					FileSaver.unsavedChanges = true;
					dialog.dispose();
				} catch (Exception e) {
					return;
				}
			}
		});

		cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				mongoClient.close();
				dialog.dispose();
			}
		});

		for (int i = 0; i < field.size(); i++) {
			field.get(i).getDocument().addDocumentListener(new existingElementFieldDocumentListener(i));
		}

		Platform.runLater(new Runnable() { // this will run initFX as JavaFX-Thread
			@Override
			public void run() {
				initFX(fxPanel, tempElementURL);
			}
		});

		mainPanel.add(mainSplit, BorderLayout.CENTER);
		mainPanel.add(bottomPanel, BorderLayout.PAGE_END);
		dialog.setContentPane(mainPanel);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		dialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent we) {
				try {
					tempFile.delete();
				} catch (Exception e) {

				}
				mongoClient.close();
				dialog.dispose();
			}
		});
		mainSplit.setDividerLocation((Toolkit.getDefaultToolkit().getScreenSize().width) / 4);
		dialog.setSize((Toolkit.getDefaultToolkit().getScreenSize().width) / 2,
				(Toolkit.getDefaultToolkit().getScreenSize().height) / 2);
		// center the dialog on screen
		dialog.setLocation((Toolkit.getDefaultToolkit().getScreenSize().width) / 2 - dialog.getWidth() / 2,
				(Toolkit.getDefaultToolkit().getScreenSize().height) / 2 - dialog.getHeight() / 2);
		root.getGlassPane().setCursor(Cursor.getDefaultCursor());
		dialog.setVisible(true);
		dialog.dispose();

	}

	private static void initFX(final JFXPanel fxPanel, String url) {
		Group group = new Group();
		Scene scene = new Scene(group);
		fxPanel.setScene(scene);
		webView = new WebView();
		webView.isResizable();
		group.getChildren().add(webView);

		// Obtain the webEngine to navigate
		webEngine = webView.getEngine();
		try {
			File f = new File(url);
			webEngine.load(f.toURI().toString());
		} catch (Exception e) {
			// TODO Auto-generated catch block

		}
	}

	private static void updateFX(String url) {
		File f;
		webView.setPrefSize(previewPane.getSize().width, previewPane.getSize().height);
		if (url == null || url.equals("") || url.equals(null)) {
			webEngine.load("htt://www.google.com");
		} else {
			f = new File(url);
			try {
				System.out.println("Updating frame");
				webEngine.load(f.toURI().toString());
			} catch (NullPointerException e) {
				// TODO Auto-generated catch block
				return;
			}
		}

	}
	private static String getFullHTML() {
		return fullHTML;
	}
	public static void updateDoc() {

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(tempFile));
			bw.write(getFullHTML());
			bw.close();
			textArea.setText(getFullHTML());
		} catch (IOException e) {

		}
		Platform.runLater(new Runnable() { // this will run initFX as JavaFX-Thread
			@Override
			public void run() {
				updateFX(tempFile.getAbsolutePath());
			}
		});
	}

	public static Element getJsoupElement() {
		return element;
	}

}
