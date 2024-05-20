import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import javax.swing.filechooser.FileNameExtensionFilter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PhotoEditorApp extends JFrame {

    private BufferedImage originalImage;
    private BufferedImage editedImage;
    private JLabel imageLabel;
    private ExecutorService executor;

    public PhotoEditorApp(){

        setTitle("Photo Edsitor");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);

        setLocationRelativeTo(null);

        createMenuBar();

        imageLabel = new JLabel();
        add(imageLabel, BorderLayout.CENTER);

        JButton grayScaleButton = new JButton("Gray Scale");
        grayScaleButton.setBackground(Color.darkGray);
        grayScaleButton.setForeground(Color.white);
        grayScaleButton.addActionListener(e -> applyGrayScaleFilter());


        JButton blurButton = new JButton("Blur");
        blurButton.setBackground(Color.darkGray);
        blurButton.setForeground(Color.white);
        blurButton.addActionListener(e -> applyBlurFilter());

        JButton edgeDetectionButton = new JButton("Edge Detection");
        edgeDetectionButton.setBackground(Color.darkGray);
        edgeDetectionButton.setForeground(Color.white);
        edgeDetectionButton.addActionListener(e -> applyEdgeDetectionFilter());

        JButton resetButton = new JButton("Reset");
        resetButton.setBackground(Color.red);
        resetButton.setForeground(Color.white);
        resetButton.addActionListener(e -> resetImage());

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(grayScaleButton);

        buttonPanel.add(blurButton);
        buttonPanel.add(edgeDetectionButton);
        buttonPanel.add(resetButton);

        add(buttonPanel, BorderLayout.SOUTH);

        setVisible(true);
      //Sukuriami threadai 3 kievienai redagavimo funkcijai
        executor = Executors.newFixedThreadPool(3);
    }


    private void createMenuBar(){
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem openItem = new JMenuItem("open");
        openItem.addActionListener(e -> openImage());

        fileMenu.add(openItem);
        menuBar.add(fileMenu);

        setJMenuBar(menuBar);
    }

    private void updateImageLabel(){

        ImageIcon imageIcon = new ImageIcon(editedImage);
        imageLabel.setIcon(imageIcon);
        imageLabel.revalidate();

    }

    private void resetImage(){

        if(originalImage != null){
            editedImage = copyImage(originalImage);
            updateImageLabel();

        }

    }

    private BufferedImage copyImage(BufferedImage image){

        ColorModel cm = image.getColorModel();
        boolean isAlphaPrm = cm.isAlphaPremultiplied();
        WritableRaster raster = image.copyData(null);
        return new BufferedImage(cm, raster, isAlphaPrm, null);

    }

    private void openImage(){
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("images", "jpg", "png"));

        int result = fileChooser.showOpenDialog(this);
        if(result == JFileChooser.APPROVE_OPTION)
        {
            File selectedFile = fileChooser.getSelectedFile();

            try
            {
                BufferedImage testImage = ImageIO.read(selectedFile);
                if(testImage == null)
                {
                    JOptionPane.showMessageDialog(this, "Invalid Image File Selected", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                originalImage = testImage;
                editedImage = copyImage(originalImage);

                updateImageLabel();
            }
            catch(Exception ex)
            {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error Loading The Image", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void applyGrayScaleFilter()
    {
        if(originalImage != null)
        {
            executor.execute(() -> {
                BufferedImage newImage = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_INT_RGB);
                for(int x = 0; x < editedImage.getWidth(); x++)
                {
                    for(int y = 0; y < editedImage.getHeight(); y++)
                    {
                        int rgb = originalImage.getRGB(x, y);
                        int gray = (int) (0.3 * ((rgb >> 16) & 0xFF) + 0.59 * ((rgb >> 8) & 0xFF) + 0.11 * (rgb & 0xFF) );
                        newImage.setRGB(x, y, (gray << 16) | (gray << 8) | gray);
                    }
                }
                editedImage = newImage;
                updateImageLabel();
            });
        }
    }

    private void applyBlurFilter() {
        if (originalImage != null) {
            executor.execute(() -> {
                int[][] kernel = {
                        {1, 1, 1, 1, 1},
                        {1, 2, 2, 2, 1},
                        {1, 2, 4, 2, 1},
                        {1, 2, 2, 2, 1},
                        {1, 1, 1, 1, 1}
                };
                int kernelSum = 28;
                for (int x = 2; x < editedImage.getWidth() - 2; x++) {
                    for (int y = 2; y < editedImage.getHeight() - 2; y++) {
                        int r = 0, g = 0, b = 0;
                        for (int i = -2; i <= 2; i++) {
                            for (int j = -2; j <= 2; j++) {
                                int rgb = originalImage.getRGB(x + i, y + j);
                                r += ((rgb >> 16) & 0xFF) * kernel[i + 2][j + 2];
                                g += ((rgb >> 8) & 0xFF) * kernel[i + 2][j + 2];
                                b += (rgb & 0xFF) * kernel[i + 2][j + 2];
                            }
                        }
                        r /= kernelSum;
                        g /= kernelSum;
                        b /= kernelSum;
                        r = Math.min(255, Math.max(0, r));
                        g = Math.min(255, Math.max(0, g));
                        b = Math.min(255, Math.max(0, b));
                        int newRgb = (r << 16) | (g << 8) | b;
                        editedImage.setRGB(x, y, newRgb);
                    }
                }
                updateImageLabel();
            });
        }
    }

    private void applyEdgeDetectionFilter()
    {
        if(originalImage != null)
        {
            executor.execute(() -> {
                int[][] sobelx = {{-1,0,1},{-2,0,2},{-1,0,1}};
                int[][] sobely = {{1,2,1},{0,0,0},{-1,-2,-1}};
                BufferedImage grayImage = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
                Graphics g = grayImage.getGraphics();
                g.drawImage(originalImage, 0, 0, null);
                g.dispose();

                for(int x = 1; x < editedImage.getWidth() - 1; x++)
                {
                    for(int y = 1; y < editedImage.getHeight() - 1; y++)
                    {
                        int gx = 0, gy = 0;
                        for(int i = -1; i <= 1; i++)
                        {
                            for(int j = -1; j <= 1; j++)
                            {
                                int gray = grayImage.getRGB(x + i, y + j) & 0xFF;
                                gx += gray * sobelx[i+1][j + 1];
                                gy += gray * sobely[i+1][j + 1];
                            }
                        }

                        int magnitude = (int) Math.sqrt(gx * gx + gy * gy);
                        int newRgb = (magnitude << 16) | (magnitude << 8) | magnitude;

                        editedImage.setRGB(x, y, newRgb);
                    }
                }
                updateImageLabel();
            });
        }
    }

    public static void main(String[] args) {
        try {
            new PhotoEditorApp();
        } catch(Exception ex) {
            System.out.println("Error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
