package image.csu.fullerton.edu;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class Image {
	
	static BufferedImage desaturateImage(BufferedImage currentImage) {
		System.out.printf("desaturateImage\n");
		BufferedImage newImage = null;
		if (currentImage != null) {
			newImage = new BufferedImage(currentImage.getWidth(),
					currentImage.getHeight(), BufferedImage.TYPE_INT_RGB);
			for (int y = 0; y < currentImage.getHeight(); y++) {
				for (int x = 0; x < currentImage.getWidth(); x++) {
					Color pixel = new Color(currentImage.getRGB(x, y));
					int b = pixel.getBlue();
					int g = pixel.getGreen();
					int r = pixel.getRed();
					int a = pixel.getAlpha();
					/* well-known formula, documented in many places */
					double grayValue = 0.3 * r + 0.59 * g + 0.11 * b;
					int gray = (0x000000FF & (int) grayValue);
					int finalGray = gray | gray << 8 | gray << 16;
					int newPixel = (a << 24) | finalGray;
					newImage.setRGB(x, y, newPixel);
				}
			}
		}
		return newImage;
	}

	static BufferedImage downscaleImage(BufferedImage currentImage, int newWidth, int newHeight) {
		System.out.printf("downscaleImage\n");
		BufferedImage scaledImage = null;
		if (currentImage != null) {
			scaledImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
			Graphics2D graphics2D = scaledImage.createGraphics();
			float scale_x = (float) newWidth / (float) currentImage.getWidth();
			float scale_y = (float) newHeight / (float) currentImage.getHeight();
			AffineTransform xform = AffineTransform.getScaleInstance(scale_x, scale_y);
			graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			graphics2D.drawImage(currentImage, xform, null);
			graphics2D.dispose();
		}
		return scaledImage;
	}

	static BufferedImage sobelEdgeDetectImage(BufferedImage currentImage) {
		/* http://users.ecs.soton.ac.uk/msn/book/new_demo/sobel/ */
		BufferedImage newImage = null;
		System.out.printf("sobelEdgeDetectImage\n");
		
		if (currentImage != null) {
			float template[] = {-1,0,1,-2,0,2,-1,0,1};
			int width, height;
			width = currentImage.getWidth();
			height = currentImage.getHeight();
			newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			
			float[] GY = new float[width*height];
			float[] GX = new float[width*height];
			int[] total = new int[width*height];
			int sum=0;
			int max=0;
			int templateSize=3;
			int[] input = new int[width*height];
			int[] output = new int[width*height];
			double[] direction = new double[width*height];
			
			currentImage.getRGB(0, 0, width, height, input, 0, width);
			
			for(int x=(templateSize-1)/2; x<width-(templateSize+1)/2;x++) {
				for(int y=(templateSize-1)/2; y<height-(templateSize+1)/2;y++) {
					sum=0;

					for(int x1=0;x1<templateSize;x1++) {
						for(int y1=0;y1<templateSize;y1++) {
							int x2 = (x-(templateSize-1)/2+x1);
							int y2 = (y-(templateSize-1)/2+y1);
							float value = (input[y2*width+x2] & 0xff) * (template[y1*templateSize+x1]);
							sum += value;
						}
					}
					GY[y*width+x] = sum;
					for(int x1=0;x1<templateSize;x1++) {
						for(int y1=0;y1<templateSize;y1++) {
							int x2 = (x-(templateSize-1)/2+x1);
							int y2 = (y-(templateSize-1)/2+y1);
							float value = (input[y2*width+x2] & 0xff) * (template[x1*templateSize+y1]);
							sum += value;
						}
					}
					GX[y*width+x] = sum;

				}
			}
			for(int x=0; x<width;x++) {
				for(int y=0; y<height;y++) {
					total[y*width+x]=(int)Math.sqrt(GX[y*width+x]*GX[y*width+x]+GY[y*width+x]*GY[y*width+x]);
					direction[y*width+x] = Math.atan2(GX[y*width+x],GY[y*width+x]);
					if(max<total[y*width+x])
						max=total[y*width+x];
				}
			}
			float ratio=(float)max/255;
			for(int x=0; x<width;x++) {
				for(int y=0; y<height;y++) {
					sum=(int)(total[y*width+x]/ratio);
					output[y*width+x] = 0xff000000 | ((int)sum << 16 | (int)sum << 8 | (int)sum);
				}
			}
			newImage.setRGB(0, 0, width, height, output, 0, width);
		}
		
		return newImage;
	}
	
	static BufferedImage edgeDetectImage(BufferedImage currentImage) {
		/*
		 * http://processing.org/learning/topics/edgedetection.html
		 * modified in that it does an inaccurate grayscale on a color image
		 */
		BufferedImage newImage = null;
		System.out.printf("edgeDetectImage\n");
		if (currentImage != null) {
			newImage = new BufferedImage(currentImage.getWidth(), currentImage.getHeight(), BufferedImage.TYPE_INT_RGB);
			float filter[][] = { { -1, -1, -1 }, { -1, 9, -1 }, { -1, -1, -1 } };
			for (int y = 1; y < currentImage.getHeight() - 1; y++) {
				for (int x = 1; x < currentImage.getWidth() - 1; x++) {
					float sum = 0;
					for (int dy = -1; dy <= 1; dy++) {
						for (int dx = -1; dx <= 1; dx++) {
							int avgVal;
							int r, g, b;
							Color pixel = new Color(currentImage.getRGB(x, y));
							b = pixel.getBlue();
							g = pixel.getGreen();
							r = pixel.getRed();
							avgVal = (b + g + r) / 3;
							float val = (float) avgVal;
							sum += filter[dy + 1][dx + 1] * val;
						}
					}
					int newPixel = (int)sum;
					if (newPixel > 255) newPixel = 255;
					if (newPixel < 0) newPixel = 0;
					Color finalPixel = new Color(newPixel,newPixel,newPixel); 
					newImage.setRGB(x, y, finalPixel.getRGB());
				}
			}
		}
		return newImage;
	}
	
	static BufferedImage cannyEdgeDetectImage(BufferedImage currentImage) {
		System.out.printf("cannyEdgeDetectImage\n");
		BufferedImage newImage = null;
		if (currentImage != null) {
			newImage = new BufferedImage(currentImage.getWidth(),
					currentImage.getHeight(), BufferedImage.TYPE_INT_RGB);
			CannyEdgeDetector detector = new CannyEdgeDetector();
			detector.setLowThreshold(0.5f);
			detector.setHighThreshold(6f);
			detector.setSourceImage(currentImage);
			detector.process();
			newImage = detector.getEdgesImage();
		}
		return newImage;
	}

	static BufferedImage thresholdImage(BufferedImage currentImage) {
		System.out.printf("thresholdImage\n");
		BufferedImage newImage = null;
		if (currentImage != null) {
			newImage = new BufferedImage(currentImage.getWidth(),
					currentImage.getHeight(), BufferedImage.TYPE_INT_RGB);
			for (int y = 0; y < currentImage.getHeight(); y++) {
				for (int x = 0; x < currentImage.getWidth(); x++) {
					int avgVal;
					int a, r, g, b;
					int finalPixel;
					Color pixel = new Color(currentImage.getRGB(x, y));
					b = pixel.getBlue();
					g = pixel.getGreen();
					r = pixel.getRed();
					a = pixel.getAlpha();
					avgVal = (b + g + r) / 3;
					/*
					 * arbitrary value, could possibly make this settable or
					 * dynamic
					 */
					if (avgVal >= 128) {
						finalPixel = 0x00FFFFFF | a << 24;
					} else {
						finalPixel = 0x00000000 | a << 24;
					}
					newImage.setRGB(x, y, finalPixel);
				}
			}
		}
		return newImage;
	}

	static void calculateMoments(BufferedImage currentImage) {
		System.out.printf("calculateMoments\n");
		if (currentImage != null) {
			ImageMoments moments = new ImageMoments(currentImage);
			for (int i=1; i <= 7 ; i++) {
				System.out.printf("  Hu[%d]: %2.2f\n", i, moments.getMoment(i));
			}
			System.out.printf("  Flusser/Suk: %2.2f\n", moments.getMoment(8));
		}
	}
	
	static void saveImage(BufferedImage currentImage, String filename, String filetype) {
		System.out.printf("saveImage(%s)[%s]\n",filename,filetype);
		try {
		    File file = new File(filename);
		    ImageIO.write(currentImage, filetype, file);
		} catch (IOException e) {
		}
	}
}
