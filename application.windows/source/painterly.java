import processing.core.*; 

import java.util.Collections; 

import java.applet.*; 
import java.awt.Dimension; 
import java.awt.Frame; 
import java.awt.event.MouseEvent; 
import java.awt.event.KeyEvent; 
import java.awt.event.FocusEvent; 
import java.awt.Image; 
import java.io.*; 
import java.net.*; 
import java.text.*; 
import java.util.*; 
import java.util.zip.*; 
import java.util.regex.*; 

public class painterly extends PApplet {



public void setup(){
  // Load image, create painterly object
  PImage img = loadImage("/samurai.jpg");
  size(img.width, img.height);
  painterly2 p = new painterly2(img);

  //**************************************************************
  // STYLE PARAMETERS
  //**************************************************************
  
  // Set brush sizes
  p.setBrushSizes(2,3,4);
  //p.setBrushSizes(new int[]{10, 5, 2, 1});
  
  // Change difference threshold
  p.setThreshold(40);
  
  // Change blur factor
  p.setBlurFactor(5);
  
  // Set minimum stroke length
  p.setMinLength(20);
  
  // Set maximum stroke length
  p.setMaxLength(100);
  
  // Set grid size
  p.setGrid(1.0f);
    
  // Set opacity
  p.setOpacity(255);
  
  // Set random jitter
  p.setJitter(30);
    
  PImage painterlied = p.paint();
  image(painterlied, 0,0);
  saveFrame("samurai2.png");
}

public void draw(){
} 

class painterly2 {
  // style parameters
  private int T; // threshold
  private int opacity; // between 0 and 255
  private int jitter;
  private int maxStrokeLength;
  private int minStrokeLength;
  private int convolutionSize;
  private float gridSize; // controls spacing of strokes
  private int[] brushSizes; // sorted, largest to smallest
  private PImage origImg;
//  Position pos = new Position(0,0);
//  int maxAE = 0;
  
  
  public painterly2(PImage img) {
    origImg = img;
  }
  
  public PImage paint() {    
    // create new constant color canvas
    PGraphics canvas = createGraphics(origImg.width, origImg.height, P2D);

    for(int j = 0; j < brushSizes.length; j++) {
      PImage refImg = convolution(origImg, convolutionSize);
      paintLayer(canvas, refImg, brushSizes[j]);
    }
    return canvas.get();
  }
  
  public void paintLayer(PGraphics canvas, PImage refImg, int r) {
    ArrayList<Stroke> strokes = new ArrayList<Stroke>();
    PImage diffImg = difference(canvas, origImg);
    int grid = PApplet.parseInt(gridSize * r);
    refImg.loadPixels();
    
    int strokeCount = 0;
    
    
    for(int i = 0; i < refImg.width; i += grid) {
      for(int j = 0; j < refImg.height; j += grid) {
        //int maxAE = 0;

        int ae = areaError(diffImg, i, j, grid);
        if(ae > T) {
          Position pos = areaErrorLoc(diffImg, i, j, grid);
          
          int c = refImg.get(i,j);
          c = color(red(c),green(c),blue(c),opacity);
          c = colorJitterize(c);
          
          Stroke s = new Stroke(pos.getX(), pos.getY(), c, r, minStrokeLength, maxStrokeLength);
          s.generateCtrlPoints(refImg, canvas);
          strokes.add(s);
          //System.out.println(s.getCtrlPoints().get(0).getX());
        } 
      }
    }
      
    Collections.shuffle(strokes);
    for(int i = 0; i < strokes.size(); i++)
      strokes.get(i).drawStroke(canvas);
  }

  public void setThreshold(int t) {
    T = t;
  }
  
  public void setBrushSizes(int minSize, int numSizes, float sizeRatio) {
    int[] brushes = new int[numSizes];
    float curBrushSize = minSize;
    for(int i = 0; i < numSizes; i++) {
      brushes[numSizes-i-1] = PApplet.parseInt(curBrushSize);
      curBrushSize *= sizeRatio;
    }
    brushSizes = brushes;
  }
  
  public void setBrushSizes(int[] brushes) {
    brushSizes = brushes;
  }
  
  public void setBlurFactor(int n) {
    convolutionSize = n;
  }
  
  public void setMaxLength(int m) {
    maxStrokeLength = m;
  }
  
  public void setMinLength(int m) {
    minStrokeLength = m;
  }
  
  public void setGrid(float g) {
    gridSize = g;
  }
  
  public void setOpacity(int p) {
    opacity = p;
  }
  
  public int colorJitterize(int c) {
    int red = PApplet.parseInt(random(red(c)-jitter, red(c)+jitter));
    int green = PApplet.parseInt(random(green(c)-jitter, green(c)+jitter));
    int blue = PApplet.parseInt(random(blue(c)-jitter, blue(c)+jitter));
    int alpha = PApplet.parseInt(alpha(c));
    if(red < 0) red = 0;
    if(green < 0) green = 0;
    if(blue < 0) blue = 0;
    if (red > 255) red = 255;
    if(green > 255) green = 255;
    if(blue > 255) blue = 255;
    return color(red,green,blue,alpha);
  }
  
  public void setJitter(int j) {
    jitter = j;
  }

  /*=======================================================================================================//
      DIFFERENCE METHOD
      -Creates difference image
  
      Inputs
      -PImage canvas: Convoluted image
      -PImage origImg: Source image
  
      Returns
      -PImage diffImg: Difference image
  //=======================================================================================================*/


  public PImage difference(PImage canvas, PImage origImg){
    PImage diff=new PImage(origImg.width, origImg.height);
    int c;
    //int count=0;
    //System.out.println(diff.pixels.length);
    for(int i=0; i<origImg.width; i++){
      for(int j=0; j<origImg.height; j++){
        c = (int) (PApplet.sqrt(PApplet.pow(red(origImg.get(i,j))-red(canvas.get(i,j)),2) + 
            PApplet.pow(green(origImg.get(i,j))-green(canvas.get(i,j)),2) + 
            PApplet.pow(blue(origImg.get(i,j))-blue(canvas.get(i,j)),2)));
        diff.pixels[j*origImg.width + i] = color(c,c,c);
        //System.out.println(Integer.toHexString(c));
      }
    }
    
    return diff;
  }


  /*========================================================================================================//
      AREA ERROR METHOD
      -Calculates area error
  
      Inputs
      -PImage diff: difference image
      -int x,y: center positions of current grid
      -int grid: size of grid whose value will be used to
       calculate the starting position using x,y
      -int maxAE: a variable that will be altered and set 
       to the biggest difference value in the grid
  
      Returns
      -int Area Error value
  //========================================================================================================*/


  public int areaError(PImage diff, int x, int y, int grid){
    float sum = 0;
    int x1 = x-grid/2;
    int y1 = y-grid/2;
    int x2 = x+grid/2;
    int y2 = y+grid/2;
    boolean isXBounds = false;
    boolean isYBounds = false;
    if(x1<0){
      x1=x;
      isXBounds = true;
    }
    if(x2>=diff.width){
      x2=x;
      isXBounds = true;
    }
    if(y1<0){
      y1=y;
      isYBounds = true;
    }
    if(y2>=diff.height){
      y2=y;
      isYBounds = true;
    }
    for(int i=x1; i<x2; i++){
      for(int j=y1; j<y2; j++){
        sum += red(diff.get(i,j));
      }
    }
    if(isXBounds){
      sum*=2;
    }
    if(isYBounds){
      sum*=2;
    }
    return (int)(sum / PApplet.pow(grid,2));

    //return sum of difference value for each pixel in m / gridSize;
  }

  public Position areaErrorLoc(PImage diff, int x, int y, int grid){
    Position pos = new Position(x,y);
    int maxAE = 0;
    int x1 = x-grid/2;
    int y1 = y-grid/2;
    int x2 = x+grid/2;
    int y2 = y+grid/2;
    boolean isXBounds = false;
    boolean isYBounds = false;
    if(x1<0){
      x1=x;
      isXBounds = true;
    }
    if(x2>=diff.width){
      x2=x;
      isXBounds = true;
    }
    if(y1<0){
      y1=y;
      isYBounds = true;
    }
    if(y2>=diff.height){
      y2=y;
      isYBounds = true;
    }
    for(int i=x1; i<x2; i++){
      for(int j=y1; j<y2; j++){
        if(maxAE<red(diff.get(i,j))){
          maxAE=(int)red(diff.get(i,j));
          pos = new Position(i,j);
        }
        //System.out.println(red(diff.get(i,j)));
      }
    }

    return pos;
  }

  /*======================================================================================================//
      CONVOLUTION METHOD
      -Blur effect
  
      Inputs
      -PImage srcImg: Original image
      -int convSize: amount of blur
  
      Returns
      -PImage edgeImg: Convoluted image
  //======================================================================================================*/

  public PImage convolution(PImage srcImg, int convSize){
    int kernDim = 1+2*convSize;
    float v = (float) (1.0f / (kernDim*kernDim));
    float[][] kernel = new float [kernDim][kernDim];
    for(int i=0; i<kernDim; i++) {
      for(int j=0; j<kernDim; j++) {
        kernel[i][j] = v;
      }
    }
 
    srcImg.loadPixels();

    // Create an opaque image of the same size as the original
    PImage edgeImg = createImage(srcImg.width, srcImg.height, RGB);

    // Loop through every pixel in the image
    for (int y = convSize; y < srcImg.height-convSize; y++) {   // Skip top and bottom edges
      for (int x = convSize; x < srcImg.width-convSize; x++) {  // Skip left and right edges
        float sumRed = 0; // Kernel sum for this pixel
        float sumGreen = 0; // Kernel sum for this pixel
        float sumBlue= 0; // Kernel sum for this pixel
        for (int ky = -convSize; ky <= convSize; ky++) {
          for (int kx = -convSize; kx <= convSize; kx++) {
            // Calculate the adjacent pixel for this kernel point
            int pos = (y + ky)*srcImg.width + (x + kx);
            // Image is grayscale, red/green/blue are identical
            float valRed = red(srcImg.pixels[pos]);
            float valGreen = green(srcImg.pixels[pos]);
            float valBlue = blue(srcImg.pixels[pos]);
            // Multiply adjacent pixels based on the kernel values
            sumRed += kernel[ky+convSize][kx+convSize] * valRed;
            sumGreen += kernel[ky+convSize][kx+convSize] * valGreen;
            sumBlue += kernel[ky+convSize][kx+convSize] * valBlue;
          }
        }
        // For this pixel in the new image, set the gray value
        // based on the sum from the kernel
        edgeImg.pixels[y*srcImg.width + x] = color(sumRed, sumGreen, sumBlue);
      }
    }
    // State that there are changes to edgeImg.pixels[]
    edgeImg.updatePixels();

    return edgeImg;
  }
    class Position {
    private int x;
    private int y;
    private float theta;
    
    /****************************************************
    CONSTRUCTORS
     ****************************************************/
    public Position(int x, int y, float theta) {
      this.x = x;
      this.y = y;
      this.theta = theta;
    }
    public Position(int x, int y) {
      this(x, y, 0);
    }
    public Position() {
      this(0, 0, 0);
    }
    
    /****************************************************
    METHODS
     ****************************************************/
    public void drawPoint(PApplet context) {
      context.point(x, y);
    }
    public void drawCircle(PApplet context, int radius) {
      context.ellipse(x, y, radius*2, radius*2);
    }
    public void drawCircle(PApplet context) {
      drawCircle(context, 5);
    }
    
    /****************************************************
    GETTERS AND SETTERS
     ****************************************************/
    public int getX() {
      return x;
    }
    public void setX(int val) {
      x = val;
    }
    public int getY() {
      return y;    
    }
    public void setY(int val) {
      y = val;
    }
    public float getTheta() {
      return theta;    
    }
    public void setTheta(float theta) {
      this.theta = theta;
    }
  }
}
class Stroke {
    public int maxStrokeLength;
    public int minStrokeLength;
    private int col;
    private ArrayList<Position> ctrlPoints;
    private int radius;

    /****************************************************
    CONSTRUCTORS
     ****************************************************/
    public Stroke(int x, int y, int col, int radius, int minLength, int maxLength) {
      this.maxStrokeLength = maxLength;
      this.minStrokeLength = minLength;
      this.col = col;
      this.ctrlPoints = new ArrayList<Position>();
      this.radius = radius;
      ctrlPoints.add(new Position(x, y));
    }
    public Stroke(int x, int y, int col) {
      this(x, y, col, 5, 2, 100);
    }
    public Stroke(int x, int y) {
      this(x, y, color(255, 0, 0, 255), 5, 2, 100);
    }
    
    /****************************************************
    METHODS
     ****************************************************/
    public void drawStroke(PGraphics context) {
      context.noStroke();
      context.fill(col);
      context.ellipse(ctrlPoints.get(0).getX(), ctrlPoints.get(0).getY(), radius*2, radius*2);
      for (int i = 1; i < ctrlPoints.size(); i++) {
        drawCtrlPointGap( ctrlPoints.get(i-1), ctrlPoints.get(i), context );
      }
    }
    /**
     * Draws in a bunch of circles between point to point.
     */
    private void drawCtrlPointGap(Position from, Position to, PGraphics context) {
      int distX =  to.getX() - from.getX();
      int distY =  to.getY() - from.getY();
      int dist = radius;

      for (int i = 1; i < dist; i++) {
        context.fill(col);
        int gapX = (int) (from.getX()+(int)(i*(distX)/((float)dist)));
        int gapY = (int) (from.getY()+(int)(i*(distY)/((float)dist)));
        context.ellipse(gapX, gapY, radius*2, radius*2);
      }
    }
    public float luminance(int col) {
      return (float) (0.30f * red(col) + .59f * green(col) + 0.11f * blue(col));
    }
    /**
     * Returns the gradient direction in radians of a certain pixel location.
     * It also handles pixels on the border of the image properly.  
     * 
     * @param Position point
     * @param PImage img
     * @return float theta
     */
    public float gradientDirection(Position point, PImage img) {
      int x = point.getX();
      int y = point.getY();
      int xbefore = x-1;
      int xafter = x+1;
      int ybefore = y-1;
      int yafter = y+1;
      if (xbefore < 0) {
        xbefore = x;
      }
      if (xafter >= img.width) {
        xafter = x;
      }
      if (ybefore < 0) {
        ybefore = y;
      }
      if (yafter >= img.height) {
        yafter = y;
      }
      float gradX = (luminance(img.get(xafter, y)) - luminance(img.get(xbefore, y)))/2;
      float gradY = (luminance(img.get(x, ybefore)) - luminance(img.get(x, yafter)))/2;
      return PApplet.atan2(gradY, gradX);
    }
    public boolean isGradientFlat(Position point, PImage img) {
      boolean isGradientFlat = false;

      int x = point.getX();
      int y = point.getY();
      int xbefore = x-1;
      int xafter = x+1;
      int ybefore = y-1;
      int yafter = y+1;
      if (xbefore < 0) {
        xbefore = x;
      }
      if (xafter >= img.width) {
        xafter = x;
      }
      if (ybefore < 0) {
        ybefore = y;
      }
      if (yafter >= img.height) {
        yafter = y;
      }
      float gradX = (luminance(img.get(xafter, y)) - luminance(img.get(xbefore, y)))/2;
      float gradY = (luminance(img.get(x, ybefore)) - luminance(img.get(x, yafter)))/2;

      if(gradX == 0 && gradY == 0) {
        isGradientFlat = true;
      }

      return isGradientFlat;
    }
    public void generateCtrlPoints(PImage refImage, PGraphics canvas) {
      if (ctrlPoints.size() != 1) {
        return;
      }
      
      Position curPoint = ctrlPoints.get(0);
      
      float curGradDirection = gradientDirection(curPoint, refImage);
      
      float normalGradDirection1, normalGradDirection2;
      curPoint.setTheta(curGradDirection + PApplet.PI/2);
      

      for (int i = 0; i < maxStrokeLength; i++) {
        Position prevPoint = ctrlPoints.get(ctrlPoints.size()-1);
        
        curPoint = new Position((int)(prevPoint.getX() + radius * PApplet.cos(prevPoint.getTheta())), (int)(prevPoint.getY() - radius * PApplet.sin(prevPoint.getTheta())));
        
        //      if (nextPoint.getX() < 0 || nextPoint.getX() >= refImage.width ||
        //          nextPoint.getY() > 0 || nextPoint.getY() >= refImage.height) {
        //        
        //      }

        if (i > minStrokeLength && 
            PApplet.abs(refImage.get(curPoint.getX(), curPoint.getY()) - canvas.get(curPoint.getX(), curPoint.getY()))
            < PApplet.abs(refImage.get(curPoint.getX(), curPoint.getY()) - col) ) {
          System.out.println("color difference too much");
          break;
        }
        if (isGradientFlat(curPoint, refImage)) {
          System.out.println("The gradient is too flat to proceed");
          break;
        }

        
        curGradDirection = gradientDirection(curPoint, refImage);
        
        normalGradDirection1 = curGradDirection + PApplet.PI / 2;
        normalGradDirection2 = curGradDirection - PApplet.PI / 2;
       
        if ( PApplet.abs(prevPoint.getTheta() - normalGradDirection1) < PApplet.abs(prevPoint.getTheta() - normalGradDirection2)) {
          curPoint.setTheta(normalGradDirection1);
        } else {
          curPoint.setTheta(normalGradDirection2);
        }
        

        ctrlPoints.add(curPoint);
      }
    }
    
      /*=======================================================================================================//
      DIFFERENCE METHOD
      -Creates difference image
  
      Inputs
      -PImage canvas: Convoluted image
      -PImage origImg: Source image
  
      Returns
      -PImage diffImg: Difference image
  //=======================================================================================================*/


  public PImage difference(PImage canvas, PImage origImg){
    PImage diff=new PImage(origImg.width, origImg.height);
    int c;
    //int count=0;
    //System.out.println(diff.pixels.length);
    for(int i=0; i<origImg.width; i++){
      for(int j=0; j<origImg.height; j++){
        c = (int) (PApplet.sqrt(PApplet.pow(red(origImg.get(i,j))-red(canvas.get(i,j)),2) + 
            PApplet.pow(green(origImg.get(i,j))-green(canvas.get(i,j)),2) + 
            PApplet.pow(blue(origImg.get(i,j))-blue(canvas.get(i,j)),2)));
        diff.pixels[j*origImg.width + i] = color(c,c,c);
        //System.out.println(Integer.toHexString(c));
      }
    }
    
    return diff;
  }


  /*========================================================================================================//
      AREA ERROR METHOD
      -Calculates area error
  
      Inputs
      -PImage diff: difference image
      -int x,y: center positions of current grid
      -int grid: size of grid whose value will be used to
       calculate the starting position using x,y
      -int maxAE: a variable that will be altered and set 
       to the biggest difference value in the grid
  
      Returns
      -int Area Error value
  //========================================================================================================*/


  public int areaError(PImage diff, int x, int y, int grid, int maxAE, Position pos){
    float sum = 0;
    maxAE = 0;
    int x1 = x-grid/2;
    int y1 = y-grid/2;
    int x2 = x+grid/2;
    int y2 = y+grid/2;
    boolean isXBounds = false;
    boolean isYBounds = false;
    if(x1<0){
      x1=x;
      isXBounds = true;
    }
    if(x2>=diff.width){
      x2=x;
      isXBounds = true;
    }
    if(y1<0){
      y1=y;
      isYBounds = true;
    }
    if(y2>=diff.height){
      y2=y;
      isYBounds = true;
    }
    for(int i=x1; i<x2; i++){
      for(int j=y1; j<y2; j++){
        sum += red(diff.get(i,j));
        if(maxAE<red(diff.get(i,j))){
          maxAE=(int)red(diff.get(i,j));
          pos = new Position(i,j);
        }
        //System.out.println(red(diff.get(i,j)));
      }
    }
    if(isXBounds){
      sum*=2;
    }
    if(isYBounds){
      sum*=2;
    }
    return (int)(sum / PApplet.pow(grid,2));

    //return sum of difference value for each pixel in m / gridSize;
  }


  /*======================================================================================================//
      CONVOLUTION METHOD
      -Blur effect
  
      Inputs
      -PImage srcImg: Original image
      -int convSize: amount of blur
  
      Returns
      -PImage edgeImg: Convoluted image
  //======================================================================================================*/

  public PImage convolution(PImage srcImg, int convSize){
    int kernDim = 1+2*convSize;
    float v = (float) (1.0f / (kernDim*kernDim));
    float[][] kernel = new float [kernDim][kernDim];
    for(int i=0; i<kernDim; i++) {
      for(int j=0; j<kernDim; j++) {
        kernel[i][j] = v;
      }
    }
    
    srcImg.loadPixels();

    // Create an opaque image of the same size as the original
    PImage edgeImg = createImage(srcImg.width, srcImg.height, RGB);

    // Loop through every pixel in the image
    for (int y = convSize; y < srcImg.height-convSize; y++) {   // Skip top and bottom edges
      for (int x = convSize; x < srcImg.width-convSize; x++) {  // Skip left and right edges
        float sumRed = 0; // Kernel sum for this pixel
        float sumGreen = 0; // Kernel sum for this pixel
        float sumBlue= 0; // Kernel sum for this pixel
        for (int ky = -convSize; ky <= convSize; ky++) {
          for (int kx = -convSize; kx <= convSize; kx++) {
            // Calculate the adjacent pixel for this kernel point
            int pos = (y + ky)*srcImg.width + (x + kx);
            // Image is grayscale, red/green/blue are identical
            float valRed = red(srcImg.pixels[pos]);
            float valGreen = green(srcImg.pixels[pos]);
            float valBlue = blue(srcImg.pixels[pos]);
            // Multiply adjacent pixels based on the kernel values
            sumRed += kernel[ky+convSize][kx+convSize] * valRed;
            sumGreen += kernel[ky+convSize][kx+convSize] * valGreen;
            sumBlue += kernel[ky+convSize][kx+convSize] * valBlue;
          }
        }
        // For this pixel in the new image, set the gray value
        // based on the sum from the kernel
        edgeImg.pixels[y*srcImg.width + x] = color(sumRed, sumGreen, sumBlue);
      }
    }
    // State that there are changes to edgeImg.pixels[]
    edgeImg.updatePixels();

    return edgeImg;
  }
    
    /****************************************************
    GETTERS AND SETTERS
     ****************************************************/
    public int getColor() {
      return col;
    }
    public void setColor(int c) {
      this.col = c;
    }
    public ArrayList<Position> getCtrlPoints() {
      return ctrlPoints;
    }
    public void setCtrlPoints(ArrayList<Position> ctrlPoints) {
      this.ctrlPoints = ctrlPoints;
    }
    public int getRadius() {
      return radius;
    }
    public void setRadius(int r) {
      this.radius = r;
    }
    public int getMaxLength() {
      return maxStrokeLength;
    }
    public void setMaxLength(int m) {
      this.maxStrokeLength = m;
    }
    public int getMinLength() {
      return minStrokeLength;
    }
    public void setMinLength(int m) {
      this.minStrokeLength = m;
    }
 // }

  class Position {
    private int x;
    private int y;
    private float theta;
    
    /****************************************************
    CONSTRUCTORS
     ****************************************************/
    public Position(int x, int y, float theta) {
      this.x = x;
      this.y = y;
      this.theta = theta;
    }
    public Position(int x, int y) {
      this(x, y, 0);
    }
    public Position() {
      this(0, 0, 0);
    }
    
    /****************************************************
    METHODS
     ****************************************************/
    public void drawPoint(PApplet context) {
      context.point(x, y);
    }
    public void drawCircle(PApplet context, int radius) {
      context.ellipse(x, y, radius*2, radius*2);
    }
    public void drawCircle(PApplet context) {
      drawCircle(context, 5);
    }
    
    /****************************************************
    GETTERS AND SETTERS
     ****************************************************/
    public int getX() {
      return x;
    }
    public void setX(int val) {
      x = val;
    }
    public int getY() {
      return y;    
    }
    public void setY(int val) {
      y = val;
    }
    public float getTheta() {
      return theta;    
    }
    public void setTheta(float theta) {
      this.theta = theta;
    }
  }
}
  static public void main(String args[]) {
    PApplet.main(new String[] { "--bgcolor=#F0F0F0", "painterly" });
  }
}
