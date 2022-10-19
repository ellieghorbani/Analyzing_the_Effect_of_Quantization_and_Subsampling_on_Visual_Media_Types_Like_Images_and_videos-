
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;	
//------------------------------------------

public class quantizationAndSubsampling {

	JFrame frame;
	JFrame frameOriginalImg;
	JFrame frameProcessedImg;
	JLabel lbImOriginqlImg;
	JLabel lbImProcessedImg;
	BufferedImage originalImg;
	BufferedImage processedImg;
	int width = 1920; // default image width and height
	int height = 1080;
	
	//represent YUV and RGB classes
	class YUV {
		double y, u, v;
		public YUV(double y,double u, double v) {
			this.y = y;
			this.u = u;
			this.v = v;
		}
	}
		
	class RGB {
		int r, g, b;
		public RGB(int r, int g, int b){
			this.r = r;
			this.g= g;
			this.b = b;
		}
	}

	/** Read Image RGB
	 * Cover Image RGB to YUV
	 * Quantization and Subsampling Image
	 * Scale and Antialiasing
	 * Display Original and Processed Image
	 */
	private void readImageRGB(int width, int height, String imgPath, BufferedImage imgO, BufferedImage imgP,int  sub_Y, int sub_U, int sub_V, float s_x, float s_y, int A)
	{
		try
		{
			int frameLength = width*height*3;

			File file = new File(imgPath);
			RandomAccessFile raf = new RandomAccessFile(file, "r");
			raf.seek(0);

			long len = frameLength;
			byte[] bytes = new byte[(int) len];

			raf.read(bytes);

			int ind = 0;
			RGB[][] originalRGB = new RGB[height][width];
			RGB[][] converedRGB = new RGB[height][width];
			YUV[][] converedYUV = new YUV[height][width];
			double[][] subsample_Y = new double[height][width/sub_Y];
			double[][] subsample_U = new double[height][width/sub_U];
			double[][] subsample_V = new double[height][width/sub_V];
			int[][] saveRGBImg = new int[height][width];
			
			for(int y = 0; y < height; y++)
			{
				for(int x = 0; x < width; x++)
				{
				    int a = 0;
					int r = bytes[ind];
					int g = bytes[ind+height*width];
					int b = bytes[ind+height*width*2]; 

					int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
					//int pix = ((a << 24) + (r << 16) + (g << 8) + b);
					imgO.setRGB(x,y,pix);
					
					r = r & 0xFF;
					g = g & 0xFF;
					b = b & 0xFF;
					
					//1. Store original RGB values
					RGB rgb = new RGB(r, g, b);
					originalRGB[y][x] = rgb;
					
					//2. Convert RGB to YUV
					double[] arrayYUV = convertRBGtoYUV(r, g, b);
					
					//3. Process YUV  down sampling
					subSampling( arrayYUV, x, y, subsample_Y, subsample_U, subsample_V, sub_Y, sub_U, sub_V);
					ind++;
				}
			}
			
			//4. adjust up sampling for display
			double[][] adjsuSampling_Y = upSampling(width, height, sub_Y, subsample_Y);
			double[][] adjsuSampling_U = upSampling(width, height, sub_U, subsample_U);
			double[][] adjsuSampling_V = upSampling(width, height, sub_V, subsample_V);
			
			//------------------------------------------------------------------------------------------------------------------------------------	
			/*
			 * For doing next steps converting images YUV to RGB and scale RGB images with respect to the choice of A, I develop two algorithms:
			 * Algorithm 1: This algorithm is based on the programming part.
                      5- Convert YUV to RGB
                      6- Scale RGB Image with respecting choice of A
                      
             * ALgorithm 2: This algorithm is based on optimization of the program.
                      5- Scale YUV Image with respecting choice of A and meanwhile convert YUV to RGB for each point.
			 */
			//-------------------------------------------------------------------------------------------------------------------------------------
			/*
			// Algorithm 1:
			//5. convert YUV to RGB
			double[][] saveR = new double[height][width];
			double[][] saveG = new double[height][width];
			double[][] saveB = new double[height][width];
			for(int y = 0; y < height; y++)
			{
				for(int x = 0; x < width; x++)
				{
					
					double R = (1.000 * adjsuSampling_Y[y][x] + 0.956 * adjsuSampling_U[y][x] + 0.621 * adjsuSampling_V[y][x]);
				    double G = (1.000 * adjsuSampling_Y[y][x] + (-0.272 * adjsuSampling_U[y][x]) + (-0.647 * adjsuSampling_V[y][x]));
				    double B = (1.000 * adjsuSampling_Y[y][x] + (-1.106 * adjsuSampling_U[y][x]) + (1.703 * adjsuSampling_V[y][x]));
				    
				    int maxx = 255; int minn = 0;
					if (R>  maxx) {R =  maxx;}
				    if (R<  minn) {R =  minn;}
				    if (G>  maxx) {G =  maxx;}
				    if (G<  minn) {G =  minn;}
				    if (B>  maxx) {B =  maxx;}
				    if (B<  minn) {B =   minn;}
					
					byte[] arrayRGB = convertYUVtoRGB(adjsuSampling_Y[y][x], adjsuSampling_U[y][x], adjsuSampling_V[y][x]);
					byte a = 0;
					byte r = (byte) arrayRGB[0];
					byte g = (byte) arrayRGB[1];
					byte b = (byte) arrayRGB[2]; 

					int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
					//int pix = ((a << 24) + (r << 16) + (g << 8) + b);
					saveRGBImg[y][x] = pix;
					saveR[y][x] = R;
					saveG[y][x] = G;
					saveB[y][x] = B;
					
				}
			}
			
			//6. Scale RGB Image with respecting choice of A
			if (A == 0) {
				for(int y = 0; y < (int) (height*s_y); y++){
					for(int x = 0; x < (int) (width*s_x); x++){
						int oldX = (int) (x/s_x);
						int oldY = (int) (y/s_y);
						imgP.setRGB(x,y,saveRGBImg[oldY][oldX]);
						}
					}
				}
			if (A == 1) {
				for(int y = 0; y < (int) (height*s_y); y++){
					for(int x = 0; x < (int) (width*s_x); x++){
						int oldX = (int) (x/s_x);
						int oldY = (int) (y/s_y);
						double addR = 0; double addG = 0; double addB = 0 ;double count = 0;
						for (int i_y = -1; i_y < 2; i_y++) {
							for (int i_x= -1; i_x < 2; i_x++) {
								int xx = oldX + i_x; int yy = oldY + i_y;
								if (xx < (width-1) &  xx >= 0 & yy < (height-1) & yy >= 0) {
									addR += saveR[yy][xx]; addG += saveG[yy][xx]; addB += saveB[yy][xx];count +=1;
								}
							}
						}
						double R = (int) (addR/count); double G =  (int) (addG/count); double B =  (int) (addB/count);
						int maxx = 255; int minn = 0;
						if (R>  maxx) {R =  maxx;}
						if (R<  minn) {R =  minn;}
						if (G>  maxx) {G =  maxx;}
					    if (G<  minn) {G =  minn;}
					    if (B>  maxx) {B =  maxx;}
					    if (B<  minn) {B =   minn;}
						int pix = 0xff000000 | (((byte) (R) & 0xff) << 16) | (((byte) (G) & 0xff) << 8) | ((byte) (B) & 0xff);
						imgP.setRGB(x,y, pix);
					}
				}
	    	}
		}
			*/
			//------------------------------------------------------------------------------------------------------------------------------------------------
			
			//ALgorithm 2: This algorithm is based on optimization of the program.
            //5- Scale YUV Image with respecting choice of A and meanwhile convert YUV to RGB for each point.
			for(int y = 0; y < (int)(height*s_y); y++)
			{   
				for(int x = 0; x < (int) (width*s_x); x++) { 
					int x_old = (int) (x/s_x); int y_old = (int) (y/s_y);
					if (A==0) {
						byte[] arrayRGB = convertYUVtoRGB(adjsuSampling_Y[y_old][x_old], adjsuSampling_U[y_old][x_old], adjsuSampling_V[y_old][x_old]);
						byte a = 0;
						byte r = (byte) arrayRGB[0];
						byte g = (byte) arrayRGB[1];
						byte b = (byte) arrayRGB[2]; 

						int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
						//int pix = ((a << 24) + (r << 16) + (g << 8) + b);
						imgP.setRGB(x,y,pix);
						ind++;
					}
					if (A==1) {
						double add_Y = 0; double add_U = 0; double add_V = 0;int count = 0;
						for (int i_y = -1; i_y < 2; i_y++) {
							for (int i_x= -1; i_x < 2; i_x++) {
								int xx = x_old + i_x; int yy = y_old + i_y;
								if (xx < (width-1) &  xx >= 0 & yy < (height-1) & yy >= 0) {
									add_Y += adjsuSampling_Y[yy][xx]; add_U += adjsuSampling_U[yy][xx]; add_V += adjsuSampling_V[yy][xx]; count +=1;
								}
							}
						}
						add_Y = (double) (add_Y/count); add_U = (double) (add_U/count); add_V = (double) (add_V/count);
						byte[] arrayRGB = convertYUVtoRGB(add_Y, add_U, add_V);
						byte a = 0;
						byte r = (byte) arrayRGB[0];
						byte g = (byte) arrayRGB[1];
						byte b = (byte) arrayRGB[2]; 

						int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
						//int pix = ((a << 24) + (r << 16) + (g << 8) + b);
						imgP.setRGB(x,y,pix);
						ind++;
					}
					
				}
			}
		}
		
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
	
	//*******************************************************************************************************************************************
	private double[] convertRBGtoYUV(int R, int G, int B) {
		double[] YUV = new double[3];
		
		double Y = (0.299 * R + 0.587 * G + 0.114 * B);
		double U = (0.596 * R + (-0.274 * G) + (-0.322 * B));
		double V = (0.211 * R + (-0.523 * G) + (0.312 * B));
		
		YUV[0] = Y;
		YUV[1] = U;
		YUV[2] = V;
		
		return YUV;
	}
	//*******************************************************************************************************************************************
	private byte[] convertYUVtoRGB(double Y, double U, double V) {
		byte[] RGB = new byte[3];
	    
	    double R = (1.000 * Y + 0.956 * U + 0.621 * V);
	    double G = (1.000 * Y + (-0.272 * U) + (-0.647 * V));
	    double B = (1.000 * Y + (-1.106 * U) + (1.703 * V));
	    
	    int maxx = 255; int minn = 0;
		if (R>  maxx) {R =  maxx;}
	    if (R<  minn) {R =  minn;}
	    if (G>  maxx) {G =  maxx;}
	    if (G<  minn) {G =  minn;}
	    if (B>  maxx) {B =  maxx;}
	    if (B<  minn) {B =   minn;}
	    	    
	    RGB[0] = (byte) R;
	    RGB[1] = (byte) G;
	    RGB[2] = (byte) B;
		
		return RGB;
	}
	//*******************************************************************************************************************************************
	private void subSampling(double[] arr, int x, int y, double[][] subsample_Y, double[][] subsample_U, double[][] subsample_V, int  Y, int U, int V) {        
        
		if(x%Y == 0) {subsample_Y[y][x/Y] = arr[0];}
		if(x%U == 0) {subsample_U[y][x/U] = arr[1];}
		if(x%V == 0) {subsample_V[y][x/V] = arr[2];}
	
        
	}
	//*******************************************************************************************************************************************
	private double[][] upSampling(int width, int height, int M, double[][] subsample){
		double[][] sample = new double[height][width];
		
		for(int i = 0 ; i < height; i++) {sample[i][0] = subsample[i][0];}
		for(int w = 1; w< subsample[0].length; w++) {
			int b = (w-1)*M;
			for (int m = 1; m< M; m++) {
				for (int h=0; h< height; h++) {
					sample[h][b+m] = (sample[h][b+m-1]+subsample[h][w])/2.0;
				}
			}
			for(int i = 0 ; i < height; i++) {sample[i][w*M] = subsample[i][w];}
		}
		
		return sample;
	}
	//*******************************************************************************************************************************************
	public void showIms(String[] args){

		// Read a parameter from command line
		int filter_Y = Integer.parseInt(args[1]);
		int filter_U = Integer.parseInt(args[2]);
		int filter_V = Integer.parseInt(args[3]);
		float scale_x = Float.parseFloat(args[4]);
		float scale_y = Float.parseFloat(args[5]);
		int A = Integer.parseInt(args[6]);
		int new_width = (int)( (float)width * scale_x);
		int new_height = (int)( (float)height * scale_y);
		
		// Read in the specified image
		originalImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		processedImg = new BufferedImage(new_width, new_height, BufferedImage.TYPE_INT_RGB);
		
		readImageRGB(width, height, args[0], originalImg, processedImg, filter_Y, filter_U, filter_V, scale_x, scale_y, A);
		
		/*
		// plot two images side by side 
		// this part does not plot two side by side images based on my system, it is Ubuntu.
		JFrame window = new JFrame();
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		BufferedImage combinedImage;
		
		int width = originalImg.getWidth() + processedImg.getWidth();
		int height = Math.max(originalImg.getHeight(), processedImg.getHeight());
		
		combinedImage = new BufferedImage(width, height,BufferedImage.TYPE_INT_ARGB);
	
		Graphics2D g = combinedImage.createGraphics();
		
		g.drawImage(originalImg, 0, 0, null);
		g.drawImage(processedImg, originalImg.getWidth()+10, 0, null);
					
					g.dispose();
					
					JLabel label = new JLabel();
					window.add(label);
					label.setIcon(new ImageIcon(combinedImage));
					window.pack();
					window.setVisible(true);
		*/
		// Use label to display the image
		frameOriginalImg = new JFrame();
		frameOriginalImg.setTitle("Original Image JFrame");
		GridBagLayout gLayout = new GridBagLayout();
		frameOriginalImg.getContentPane().setLayout(gLayout);

		lbImOriginqlImg = new JLabel(new ImageIcon(originalImg));

		GridBagConstraints c1 = new GridBagConstraints();
		c1.fill = GridBagConstraints.HORIZONTAL;
		c1.anchor = GridBagConstraints.CENTER;
		c1.weightx = 0.5;
		c1.gridx = 0;
		c1.gridy = 0;

		c1.fill = GridBagConstraints.HORIZONTAL;
		c1.gridx = 0;
		c1.gridy = 1;
		frameOriginalImg.getContentPane().add(lbImOriginqlImg, c1);
		frameOriginalImg.pack();
		frameOriginalImg.setVisible(true);
		frameOriginalImg.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		//------------------------------------------------------------------------
		frameProcessedImg = new JFrame();
		frameProcessedImg.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frameProcessedImg.setTitle("Processed Image JFrame");
		GridBagLayout gLayout2 = new GridBagLayout();
		frameProcessedImg.getContentPane().setLayout(gLayout2);

		lbImProcessedImg = new JLabel(new ImageIcon(processedImg));

		GridBagConstraints c2 = new GridBagConstraints();
		c2.fill = GridBagConstraints.HORIZONTAL;
		c2.anchor = GridBagConstraints.CENTER;
		c2.weightx = 0.5;
		c2.gridx = 0;
		c2.gridy = 0;

		c2.fill = GridBagConstraints.HORIZONTAL;
		c2.gridx = 0;
		c2.gridy = 1;
		frameProcessedImg.getContentPane().add(lbImProcessedImg, c2);
		frameProcessedImg.pack();
		frameProcessedImg.setVisible(true);
		frameProcessedImg.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		}
	
	

	public static void main(String[] args) {
		quantizationAndSubsampling ren = new quantizationAndSubsampling();
		ren.showIms(args);
		
	}

}
