package primitives;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.awt.geom.*;
import java.awt.image.*;

import geom.*;
import dialogs.*;
import export.*;
import globals.*;

/** Class to handle the advanced text primitive.

<pre>
	This file is part of FidoCadJ.

    FidoCadJ is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    FidoCadJ is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with FidoCadJ.  If not, see <http://www.gnu.org/licenses/>.

	Copyright 2007-2010 by Davide Bucci
</pre>

@author Davide Bucci
*/

public class PrimitiveAdvText extends GraphicPrimitive
{

	private String txt;
	private int six;
	private int siy;
	private int sty;
	private int o;
	private String fontName;
	
	private boolean recalcSize;

	/* Text style patterns */
	static final int TEXT_BOLD=1;
	static final int TEXT_ROTATE=2;
	static final int TEXT_MIRRORED=4;
	
	// A text is defined by one point.
	static final int N_POINTS=1;
		
	/** Gets the number of control points used.
		@return the number of points used by the primitive
	*/
	
	public int getControlPointNumber()
	{
		return N_POINTS;
	}
	/** Standard "empty" constructor.
	
	*/
	public PrimitiveAdvText()
	{
		super();
		six=3;
		siy=4;
		o=0;
		txt="";
		fontName = Globals.defaultTextFont;
		virtualPoint = new Point[N_POINTS];
		for(int i=0;i<N_POINTS;++i)
			virtualPoint[i]=new Point();
		
		changed=true;
		recalcSize=true;

		
	}
	
	/** Complete constructor.
		@param x the x position of the control point of the text
		@param y the y position of the control point of the text
		@param sx the x size of the font
		@param sy the y size of the font
		@param fn font name
		@param or the orientation of the text
		@param st the style of the text
		@param t the text to be used
		@param l the layer to be used
	*/
	public PrimitiveAdvText(int x, int y, int sx, int sy, String fn, int or, int st,
							String t, int l)
	{
		this();
		virtualPoint[0]=new Point(x,y);
		six=sx;
		siy=sy;
		sty=st;
		txt=t;
		fontName=fn;
		setLayer(l);
		changed=true;
		recalcSize=true;
	}

	private	AffineTransform at;
	private AffineTransform stretching;
	private AffineTransform ats;
	private Font f;
	private boolean mirror;
	private int orientation;
	private AffineTransform mm;
	private FontMetrics fm;
	private int h, th, w, hh, ww;
	private int x1, y1, xa, ya, qq;
	private double xyfactor, si, co;
	
	/** Draw the graphic primitive on the given graphic context.
		@param g the graphic context in which the primitive should be drawn.
		@param coordSys the graphic coordinates system to be applied.
		@param layerV the layer description.
	*/
	final public void draw(Graphics2D g, MapCoordinates coordSys,
							  ArrayList layerV)
	{
		if(!selectLayer(g,layerV))
			return;
			
		// For this:
		// http://sourceforge.net/tracker/?func=detail&aid=2908420&group_id=274886&atid=1167997
		// we are now checking if the text is "" before printing it.
			
		if(txt.length()==0)
			return;		
		
		if(changed) {
 			changed=false;
			mirror=false;
 			recalcSize =true;
 			/* in the simple text primitive, the the virtual point represents
		   	the position of the text to be drawn. */
			x1=virtualPoint[0].x;
 			y1=virtualPoint[0].y;
 			xa=coordSys.mapX(x1,y1);
 			ya=coordSys.mapY(x1,y1);
 			/* siy is the font horizontal size in mils (1/1000 of an inch).
 		   	1 typographical point is 1/72 of an inch.
 			*/
 			
 			f=new Font(fontName,((sty & 
 				TEXT_BOLD)==0)?Font.PLAIN:Font.BOLD,
 				(int)(six*12*coordSys.getYMagnitude()/7+.5));
 			 
			/* At first, I tried to use an affine transform on the font, without
		   	pratically touching the graphic context. This technique worked well,
		   	but I noticed it produced bugs on the case of a jar packed on a 
		   	MacOSX application bundle.
		   	I therefore choose (from v. 0.20.2) to use only graphic context
		   	transforms. What a pity!
		   
		   	February 20, 2009: I noticed this is in fact a bug on JRE < 1.5 
		   
		   	*/
	    	
    		orientation=o;
    	
    		if((sty & TEXT_MIRRORED)!=0){
    	 		mirror=true;
    	 		orientation=-orientation;
    		}
    		if (six==0 || siy==0) {
    			siy=10;
    			six=7;
    		}
    	
			fm = g.getFontMetrics(f);
    		h = fm.getAscent();
    		th = h+fm.getDescent();
    		w = fm.stringWidth(txt);
    				
 			xyfactor=1.0;
 			stretching = new AffineTransform();

 		
 			if(siy/six != 10/7){
    			// Create a transformation for the font. 
				xyfactor=(double)siy/(double)six*22.0/40.0; 	
   			}
 				
    		if(orientation!=0){
    			si=Math.sin(Math.toRadians(-orientation));
				co=Math.cos(Math.toRadians(-orientation));
    		
    			if(mirror) {
    				mm = new AffineTransform(); 
    				mm.scale(-1,1);
    				stretching.scale(1,xyfactor);
				
    			} else {
    				stretching.scale(1,xyfactor);
				}
    		
				hh=(int)(w*si+th*co);
				ww=(int)(w*co-th*si);
							
   				coordSys.trackPoint(xa,ya);
  				coordSys.trackPoint(xa+ww,ya+th);
   				coordSys.trackPoint(xa,ya+hh);
   				coordSys.trackPoint(xa+ww,ya+hh+th);	
			} else {
  				if (!mirror){
  					coordSys.trackPoint(xa+w,ya);
					coordSys.trackPoint(xa,ya+(int)(h/xyfactor));
					stretching.scale(1,xyfactor);
				} else {
					coordSys.trackPoint(xa-w,ya);
					coordSys.trackPoint(xa,ya+(int)(th/xyfactor));
				}
			}
    		qq=(int)(ya/xyfactor);
		}
	   	
   		at=(AffineTransform)g.getTransform().clone();
		ats=(AffineTransform)g.getTransform().clone();
	
		if(orientation!=0){
    		if(mirror) {
    			// Here the text is rotated and mirrored
    		    at.concatenate(mm);
				at.rotate(Math.toRadians(orientation),-xa, ya);
				at.concatenate(stretching);
   				g.setTransform(at);
				if(!g.getFont().equals(f))
	   				g.setFont(f);

    			g.drawString(txt,-xa,qq+h); 

    		} else {
    			// Here the text is just rotated
				at.rotate(Math.toRadians(-orientation),xa,ya);
				at.concatenate(stretching);
   				g.setTransform(at);
   				if(!g.getFont().equals(f))
	   				g.setFont(f);
				g.drawString(txt,xa,qq+h); 
    		}
  		} else {
			if (!mirror){
				// Here the text is normal
				at.concatenate(stretching);
				g.setTransform(at);
				
				if(g.hitClip(xa,qq, w, th)){
					if(th<Globals.textSizeLimit) {
						g.drawLine(xa,qq,xa+w,qq);
						g.setTransform(ats);
	   					return;
	    			} else {
	    				if(!g.getFont().equals(f))
	   						g.setFont(f);
						g.drawString(txt,xa,qq+h);
					}
				}
			} else {
				// Here the text is mirrored
				at.scale(-1,xyfactor);
				g.setTransform(at);
				if(g.hitClip(-xa,qq,w,h)){
					if(!g.getFont().equals(f))
	   					g.setFont(f);

					g.drawString(txt,-xa,qq+h); 
				}
			}
		}
		g.setTransform(ats);
	}
	
	/**	Parse a token array and store the graphic data for a given primitive
		Obviously, that routine should be called *after* having recognized
		that the called primitive is correct.
		That routine also sets the current layer.
		
		@param tokens the tokens to be processed. tokens[0] should be the
		command of the actual primitive.
		@param N the number of tokens present in the array
		
	*/
	public void parseTokens(String[] tokens, int N)		
		throws IOException
	{
		// assert it is the correct primitive
		changed=true;
		recalcSize = true;
		
 		if (tokens[0].equals("TY")) {	// Text (advanced)
 			if (N<9) {
 				IOException E=new IOException("bad arguments on TY");
				throw E;
			}
			
 			virtualPoint[0].x=Integer.parseInt(tokens[1]);
 			virtualPoint[0].y=Integer.parseInt(tokens[2]);
 			siy=Integer.parseInt(tokens[3]);
 			six=Integer.parseInt(tokens[4]);
 			o=Integer.parseInt(tokens[5]);
 			sty=Integer.parseInt(tokens[6]);
 			parseLayer(tokens[7]);
 			
 			int j=8;
			StringBuffer txtb=new StringBuffer();
      		
      		if(tokens[8].equals("*")) {
      			fontName = Globals.defaultTextFont;
      		} else {
      			fontName = tokens[8].replaceAll("\\+\\+"," ");
      		}
  
 			
 			/* siy is the font horizontal size in mils (1/1000 of an inch).
 			   1 typographical point is 1/72 of an inch.
			*/
 		 					
      		while(j<N-1){
      			txtb.append(tokens[++j]);
      			if (j<N-1) txtb.append(" ");
      		}
			txt=txtb.toString();
      		
 					
			
 		} else if (tokens[0].equals("TE")) {	// Text (simple)
 			if (N<4) {
 				IOException E=new IOException("bad arguments on TE");
				throw E;
			}
			
 			virtualPoint[0].x=Integer.parseInt(tokens[1]);
 			virtualPoint[0].y=Integer.parseInt(tokens[2]);
 			// Default sizes and styles
 			six=3;
 			siy=4;
 			o=0;
 			sty=0;
 			
 			int j=2;
			txt="";
      				
      		while(j<N-1)
      			txt+=tokens[++j]+" ";
      		
 			// In the original simple text primitive, the layer was not
 			// handled. Here we just suppose it is always 0.
 			
 			parseLayer("0");
			
 		} else {
 			IOException E=new IOException("Invalid primitive:"+
 										  " programming error?");
			throw E;
 		} 			
 	
	}
	
	private static BufferedImage sizeCalculationImage;
	private int xaSCI;
	private int yaSCI;
	private int orientationSCI;
	private int hSCI, thSCI, wSCI;
	private double[] xpSCI, ypSCI;
	
	/** Gets the distance (in primitive's coordinates space) between a 
	    given point and the primitive. 
	    When it is reasonable, the behaviour can be binary (polygons, 
	    ovals...). In other cases (lines, points), it can be proportional.
		@param px the x coordinate of the given point
		@param py the y coordinate of the given point
	*/
	public int getDistanceToPoint(int px, int py)
	{ 					
		// This calculation takes a lot of time, since we need to obtain the
		// size of the font used, calculate the area which is active for the 
		// mouse and so on. For this reason, we make it only when necessary, 
		// by exploiting exactly the same principle of the optimized draw
		// routines.
		
		if (changed||recalcSize) {
 			// recalcSize is set to true when the draw method detects that the
 			// graphical appearance of the text should be recalculated.
 			
 			recalcSize = false;
        	sizeCalculationImage = new BufferedImage(1, 1, 
        		BufferedImage.TYPE_INT_RGB);
     
         	// Create a graphics contents on the buffered image
         	Graphics2D gSCI = sizeCalculationImage.createGraphics();
 		
			xaSCI=virtualPoint[0].x;
 			yaSCI=virtualPoint[0].y;

    		orientationSCI=o;
 			
 			Font f=new Font(fontName,((sty & 
 				TEXT_BOLD)==0)?Font.PLAIN:Font.BOLD,
 				(int)(six*12/7+.5));
 			
	   		gSCI.setFont(f);
            
			FontMetrics fmSCI = gSCI.getFontMetrics(f);
    		hSCI = fmSCI.getAscent();
    		thSCI = hSCI+fmSCI.getDescent();
   			wSCI = fmSCI.stringWidth(txt);
	
		
			if(siy/six != 10/7){
    			hSCI=h*Math.round((int)((double)siy*24.0/40.0/six)); 
				thSCI=th*Math.round((int)((double)siy*24.0/40.0/six)); 

   			}
   			// Corrections for the mirrored text.
 			if((sty & TEXT_MIRRORED)!=0){
    	 		orientationSCI=-orientationSCI;
    	 		wSCI=-wSCI;
    		}	
 			
 			// If there is a tilt of the text, we calculate the four corner
 			// of the tilted text area and we put them in a polygon.
    		if(orientationSCI!=0){
    			double si=Math.sin(Math.toRadians(orientation));
				double co=Math.cos(Math.toRadians(orientation));
		
 				xpSCI=new double[4];
        		ypSCI=new double[4];
                        
        
        		xpSCI[0]=xaSCI;
            	ypSCI[0]=yaSCI;
            	xpSCI[1]=(int)(xaSCI+thSCI*si);
            	ypSCI[1]=(int)(yaSCI+thSCI*co);
            	xpSCI[2]=(int)(xaSCI+thSCI*si+wSCI*co);
            	ypSCI[2]=(int)(yaSCI+thSCI*co-wSCI*si);
            	xpSCI[3]=(int)(xaSCI+wSCI*co);
            	ypSCI[3]=(int)(yaSCI-wSCI*si);
            }
        }

       	if(orientationSCI!=0){	
       		if(GeometricDistances.pointInPolygon(4,xpSCI,ypSCI, px,py))
          		return 0;
		} else {
			if(GeometricDistances.pointInRectangle(Math.min(xaSCI, 
				xaSCI+wSCI),yaSCI,Math.abs(wSCI),thSCI,px,py))
	           	return 0;
		}	
		return Integer.MAX_VALUE;
		
	}	
	
	/**	Get the control parameters of the given primitive.
	
		@return a vector of ParameterDescription containing each control
				parameter.
				The first parameters should always be the virtual points.
				
	*/
	public Vector getControls()
	{
		Vector v = new Vector(10);
		int i;
		ParameterDescription pd = new ParameterDescription();

		pd.parameter=txt;
		pd.description="Text:";
		v.add(pd);
						
		for (i=0;i<getControlPointNumber();++i) {
			pd = new ParameterDescription();
			pd.parameter=virtualPoint[i];
			pd.description="Control point "+(i+1)+":";
			v.add(pd);
		}
		
		pd = new ParameterDescription();
		pd.parameter=new LayerInfo(getLayer());
		pd.description="Layer:";
		v.add(pd);
		
		pd = new ParameterDescription();
		pd.parameter=new Integer(six);
		pd.description="X Size:";
		v.add(pd);
		
		pd = new ParameterDescription();
		pd.parameter=new Integer(siy);
		pd.description="Y Size:";
		v.add(pd);
		
	 	pd = new ParameterDescription();
		pd.parameter=new Integer(o);
		pd.description="Angle:";
		v.add(pd);
		pd = new ParameterDescription();
		pd.parameter=new Boolean((sty & TEXT_MIRRORED)!=0);
		pd.description="Mirror";
		v.add(pd);
		pd = new ParameterDescription();
		pd.parameter=new Boolean((sty & TEXT_BOLD)!=0);
		pd.description="Boldface";
		v.add(pd);
		pd = new ParameterDescription();
		pd.parameter=new Font(fontName,Font.PLAIN,12);
		pd.description="Font:";
		v.add(pd);
		return v;
	}
	/**	Set the control parameters of the given primitive.
		This method is specular to getControls().
		
		@param v a vector of ParameterDescription containing each control
				parameter.
				The first parameters should always be the virtual points.
				
	*/
	public void setControls(Vector v)
	{
		int i=0;
		changed=true;
		recalcSize = true;
		ParameterDescription pd;
		
		pd=(ParameterDescription)v.get(i);
		++i;
		// Check, just for sure...
		if (pd.parameter instanceof String)
			txt=(String)pd.parameter;
		else
		 	System.out.println("Warning: unexpected parameter!"+pd);

		for (i=1;i<getControlPointNumber()+1;++i) {
			pd = (ParameterDescription)v.get(i);
			
			// Check, just for sure...
			if (pd.parameter instanceof Point)
				virtualPoint[i-1]=(Point)pd.parameter;
			else
			 	System.out.println("Warning: unexpected parameter!");
			
		}
		pd = (ParameterDescription)v.get(i);
		++i;
		// Check, just for sure...
		if (pd.parameter instanceof LayerInfo)
			setLayer(((LayerInfo)pd.parameter).getLayer());
		else
		 	System.out.println("Warning: unexpected parameter!");		
		pd=(ParameterDescription)v.get(i);
		++i;
		if (pd.parameter instanceof Integer)
			six=((Integer)pd.parameter).intValue();
		else
		 	System.out.println("Warning: unexpected parameter!"+pd);
		
		pd=(ParameterDescription)v.get(i);
		++i;
		if (pd.parameter instanceof Integer)
			siy=((Integer)pd.parameter).intValue();
		else
		 	System.out.println("Warning: unexpected parameter!"+pd);
		
		pd=(ParameterDescription)v.get(i);
		++i;
		if (pd.parameter instanceof Integer)
			o=((Integer)pd.parameter).intValue();
		else
		 	System.out.println("Warning: unexpected parameter!"+pd);
		
		
		pd=(ParameterDescription)v.get(i++);
		if (pd.parameter instanceof Boolean)
			sty = (((Boolean)pd.parameter).booleanValue())?
				sty&sty | TEXT_MIRRORED:
				sty&sty & (~TEXT_MIRRORED);
		else
		 	System.out.println("Warning: unexpected parameter!"+pd);
		pd=(ParameterDescription)v.get(i++);
		if (pd.parameter instanceof Boolean)
			
			sty= (((Boolean)pd.parameter).booleanValue()) ? 
				sty&sty | TEXT_BOLD:
				sty&sty & (~TEXT_BOLD);
		else
		 	System.out.println("Warning: unexpected parameter!"+pd);
		
		pd=(ParameterDescription)v.get(i++);
		if (pd.parameter instanceof Font)
			fontName = ((Font)pd.parameter).getFamily();
		else
		 	System.out.println("Warning: unexpected parameter!"+pd);
		
	}
	
	/** Rotate the primitive. Here we just rotate 90� by 90�
	
		@param bCounterClockWise specify if the rotation should be done 
				counterclockwise.
	*/
	public void rotatePrimitive(boolean bCounterClockWise, int ix, int iy)
	{
		super.rotatePrimitive(bCounterClockWise, ix, iy);
		
		int po=o/90;
		
		if((sty & TEXT_MIRRORED)!=0) {
			bCounterClockWise=!bCounterClockWise;
		}
		
		if (bCounterClockWise)
			po=++po%4;
		else
			po=(po+3)%4;
	
		o=90*po;
	}
	
	/** Mirror the primitive. For the text, it is different than for the other
		primitives, since we just need to toggle the mirror flag.
		@param xPos is the symmetry axis
		
	*/
	public void mirrorPrimitive(int xPos)
	{
		super.mirrorPrimitive(xPos);
		sty ^= TEXT_MIRRORED;
		changed=true;
		recalcSize = true;
	}
	
	/** Obtain a string command descripion of the primitive.
		@return the FidoCad code corresponding to the primitive.
	*/
	public String toString(boolean extensions)
	{
		String subsFont;
		
		// The standard font is indicated with an asterisk
		if (fontName.equals(Globals.defaultTextFont)) {
			subsFont = "*";
		} else {
			StringBuffer s=new StringBuffer("");
    		// All spaces are substituted with "++" in order to avoid problems
    		// during the parsing phase
    		for (int i=0; i<fontName.length(); ++i) {
    		if(fontName.charAt(i)!=' ') 
    			s.append(fontName.charAt(i));
    		else
    			s.append("++");
    		}
			subsFont=s.toString();
		}
		
		String s= "TY "+virtualPoint[0].x+" "+virtualPoint[0].y+" "+siy+" "
			+six+" "+o+" "+sty+" "+getLayer()+" "+subsFont+" "+txt+"\n";
	
		return s;
	}
	
	public void export(ExportInterface exp, MapCoordinates cs) 
		throws IOException
	{
		exp.exportAdvText (cs.mapX(virtualPoint[0].x,virtualPoint[0].y),
			cs.mapY(virtualPoint[0].x,virtualPoint[0].y), six, siy,
			fontName, 
			(sty & TEXT_BOLD)!=0,
			(sty & TEXT_MIRRORED)!=0,
			false,
			o, getLayer(), txt);
			
	}

}