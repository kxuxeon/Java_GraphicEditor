package frames;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.util.Vector;

import javax.swing.JPanel;

import frames.GMainFrame.ToolOptionActionHandler;
import main.GConstants;
import main.GConstants.ECursor;
import main.GConstants.EEditMenu;
import main.GConstants.EToolbar;
import main.GDeepClone;
import menus.GPopupMenu;
import shape.GAnchors.EAnchors;
import shape.GGroup;
import shape.GImageRectangle;
import shape.GSelect;
import shape.GShape;
import shape.GShape.EDrawingStyle;
import transformer.GDrawer;
import transformer.GMover;
import transformer.GResizer;
import transformer.GRotator;
import transformer.GTransformer;

public class GDrawingPanel extends JPanel{
	private static final long serialVersionUID = GConstants.serialVersionUID;
	
	private GPopupMenu popupMenu;
	
	private enum EDrawingState {eIdle, eDrawing, eTransforming}
	private EDrawingState eDrawingState;	
	
	private GTransformer transformer;
	private GDeepClone deepCloner;
	private GClipboard clipboard;
	private MouseHandler mouseHandler;
	private Vector<GShape> shapes, copyShapes, onShapes;
	private GShape currentShape, currentTool;
	
	private Color lineColor, fillColor;
	private boolean bUpdated, bselect;
	
	public GDrawingPanel(ToolOptionActionHandler toolOptionActionHandler) {
		this.setBackground(GConstants.eBGColor);
		this.setForeground(GConstants.eFGColor);
		
		this.eDrawingState = EDrawingState.eIdle;
		this.bUpdated = false;
		this.mouseHandler = new MouseHandler();
		this.addMouseListener(this.mouseHandler);
		this.addMouseMotionListener(this.mouseHandler);

		this.shapes = new Vector<GShape>();
		this.copyShapes = new Vector<GShape>();
		this.onShapes = new Vector<GShape>();
		
		this.currentShape = null;
		this.transformer = null;
		this.clipboard = new GClipboard();
		this.deepCloner = new GDeepClone();
		this.bselect = false;
		
		this.popupMenu = new GPopupMenu(new ActionHandler(),toolOptionActionHandler);
		this.add(this.popupMenu);
		
	}
	public void initialize() {
		this.lineColor = this.getForeground();
		this.fillColor = this.getBackground(); 
	}
	public boolean isBselect() {return bselect;}
	public void setBselect(boolean bselect) {this.bselect = bselect;}
	
	public void setCurrentTool(EToolbar eToolBar) {this.currentTool = eToolBar.getTool();}
	
	// Get&Set&Clear Vector<GShape>
	public Vector<GShape> getShapes() {return this.shapes;}
	@SuppressWarnings("unchecked")
	public void setShapes(Object shapes) {
		Graphics2D graphics2D = (Graphics2D)this.getGraphics();
		this.shapes = (Vector<GShape>) shapes;
		for (GShape shape: this.shapes) {shape.draw(graphics2D);}
	}
	public void clearShapes() {
		this.shapes.clear();
		this.repaint();
		this.bUpdated = false;
	}
	
	// Update Flag
	public boolean isUpdated() {return this.bUpdated;}
	public void setUpdated(boolean updated) {this.bUpdated = updated;}
	
	// Set Colors
	public void setLineColor(Color lineColor) {this.lineColor = lineColor;}
	public void setFillColor(Color fillColor) {this.fillColor = fillColor;}
	
	// Override 'paint' Method
	public void paint(Graphics graphics) {
		Graphics2D g2d = (Graphics2D)graphics;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
		super.paint(g2d);
		for (GShape shape: this.shapes) {shape.draw(g2d);}
	}
	
	// Check points are in the shape
	private GShape onShape(int x, int y) {
		this.onShapes.clear();
			for(GShape shape : this.shapes) {
				if (shape.contains(x,y)) {
					this.onShapes.add(shape);
				}
			}
			if (this.onShapes.isEmpty()) {return null;}
			return this.onShapes.lastElement();
	}
	private void setSelected(GShape selectedShape) {
		if (!bselect) {
			for(GShape Shape : shapes) {Shape.setSelected(false);}
		}
		if (selectedShape != null) {
			selectedShape.setSelected(true);
		}
	}
	public void addImage(File imageFile) {
		GImageRectangle gImageRectangle = new GImageRectangle(imageFile);
		
		this.shapes.add(gImageRectangle);
		this.repaint();
	}
	
	// Transforming Tool
	private void initTransforming(GShape shape, int x, int y) {
		this.requestFocus();
		this.clipboard.setContents(this.shapes);
		if (shape == null) {
			// drawing
			this.currentShape = this.currentTool.clone();
			this.currentShape.setLineColor(this.lineColor);
			this.currentShape.setFillColor(this.fillColor);

			this.transformer = new GDrawer(this.currentShape);
		}else {
			this.currentShape = shape;
			switch(shape.getESelectedAnchor()) {
			case MM:
				this.transformer = new GMover(this.currentShape);
				break;
			case RR:
				this.transformer = new GRotator(this.currentShape);
				break;
			default:
				this.transformer = new GResizer(this.currentShape);
				break;				
			}
		}
		this.transformer.initTransforming((Graphics2D)this.getGraphics(),x,y);
	}
	
	private void keepTransforming(int x, int y) {
		Graphics2D g2d = (Graphics2D)this.getGraphics();
		g2d.setXORMode(getBackground());
		this.transformer.keepTransforming(g2d,x, y);
	}
	private void continueTransforming(int x, int y) {
		Graphics2D g2d = (Graphics2D)this.getGraphics();
		g2d.setXORMode(getBackground());
		this.transformer.continueTransforming(g2d,x, y);
	}
	private void finishTransforming(int x, int y) {
		if (this.currentShape instanceof GSelect) {((GSelect)this.currentShape).contains(this.shapes);}
		else if(this.transformer instanceof GDrawer){this.shapes.add(this.currentShape);}
		this.transformer.finishTransforming((Graphics2D)this.getGraphics(),x, y);
	}
	
	// Mouse Cursor Tool
	private void checkCursor(int x, int y) {
		GShape selectedShape = this.onShape(x, y);
		if (this.onShape(x, y) == null) {
			this.setCursor(ECursor.eDefault.getCursor());
		} else {
			EAnchors eSelectedAnchor = selectedShape.getESelectedAnchor();
			switch (eSelectedAnchor) {
			case NW:this.setCursor(ECursor.eNW.getCursor()); break;
			case NN:this.setCursor(ECursor.eNN.getCursor()); break;
			case NE:this.setCursor(ECursor.eNE.getCursor()); break;
			case EE:this.setCursor(ECursor.eEE.getCursor()); break;
			case SE:this.setCursor(ECursor.eSE.getCursor()); break;
			case SS:this.setCursor(ECursor.eSS.getCursor()); break;
			case SW:this.setCursor(ECursor.eSW.getCursor()); break;
			case WW:this.setCursor(ECursor.eWW.getCursor()); break;
			case RR:this.setCursor(ECursor.eRotate.getCursor()); break;
			case MM:this.setCursor(ECursor.eMove.getCursor()); break;
			default: break;
			}
		}	
	}

	// Drawing Tool
	public void undo() {
		this.clipboard.setlastContents(this.shapes);
		this.shapes = this.clipboard.getContents(EEditMenu.eUndo.getActionCommand());
		this.repaint();
	}
	public void redo() {
		this.clipboard.setContents(this.shapes);
		this.shapes = this.clipboard.getContents(EEditMenu.eRedo.getActionCommand());
			this.repaint();
	}
	public void copy() {
		this.copyShapes.clear();
		for(GShape selectedShape : this.shapes) {
			if (selectedShape.isSelected()) {
				this.copyShapes.add((GShape) this.deepCloner.clone(selectedShape));
			}
		}
	}
	public void cut() {
		this.clipboard.setContents(this.shapes);
		this.copyShapes.clear();
		for(int i= this.shapes.size()-1; i>=0;i--) {
			if(this.shapes.get(i).isSelected()) {
				this.copyShapes.add((GShape) this.deepCloner.clone(this.shapes.get(i)));
				this.shapes.remove(i);
			}
		}
		this.repaint();
	}
	public void paste() {
		this.clipboard.setContents(this.shapes);
		for(GShape Shape : shapes) {Shape.setSelected(false);}
		for (GShape copyShape : this.copyShapes) {
			AffineTransform at = new AffineTransform();
			at.setToTranslation(5, 5);
			copyShape.setShape(at.createTransformedShape(copyShape.getShape()));
			this.shapes.add((GShape) this.deepCloner.clone(copyShape));
			copyShape.setSelected(true);
		}
		this.repaint();
	}
	public void forward() {
		this.clipboard.setContents(this.shapes);
		GShape selectedShape = null;
		GShape changedShape = null;
		for (int i = 0; i < this.shapes.size(); i++) {
			if (this.shapes.get(i).isSelected()) {
				if (i==this.shapes.size()-1) {return;}
				else {
					selectedShape = this.shapes.get(i);
					changedShape = this.shapes.get(i+1);
				}
			}
		}
		this.shapes.setElementAt(selectedShape, this.shapes.indexOf(selectedShape)+1);
		this.shapes.setElementAt(changedShape, this.shapes.indexOf(selectedShape));
		this.repaint();
	}
	public void backward() {
		this.clipboard.setContents(this.shapes);
		GShape selectedShape = null;
		GShape changedShape = null;
		for (int i = 0; i < this.shapes.size(); i++) {
			if (this.shapes.get(i).isSelected()) {
				if (i==0) {return;}
				else {
					selectedShape = this.shapes.get(i);
					changedShape = this.shapes.get(i-1);
				}
			}
		}
		this.shapes.setElementAt(changedShape, this.shapes.indexOf(changedShape)+1);
		this.shapes.setElementAt(selectedShape, this.shapes.indexOf(changedShape));
		System.out.println(this.shapes.indexOf(selectedShape));
		System.out.println(this.shapes.indexOf(changedShape));
		this.repaint();
	}
	public void sForward() {
		this.clipboard.setContents(this.shapes);
		GShape selectedShape = null;
		for (int i = 0; i < this.shapes.size(); i++) {
			if (this.shapes.get(i).isSelected()) {
				if (i==this.shapes.size()-1) {return;}
				else {selectedShape = this.shapes.get(i);}
			}
		}
		this.shapes.remove(selectedShape);
		this.shapes.add(selectedShape);
		this.repaint();
	}
	public void sBackward() {
		this.clipboard.setContents(this.shapes);
		GShape selectedShape = null;
		for (int i = 0; i < this.shapes.size(); i++) {
			if (this.shapes.get(i).isSelected()) {
				if (i==0) {return;}
				else {
					selectedShape = this.shapes.get(i);
					this.shapes.remove(i);
				}
			}
		}
		Vector<GShape> newShapes = new Vector<GShape>();
		newShapes.add(selectedShape);
		for (int i = 0; i < this.shapes.size(); i++) {newShapes.add(this.shapes.get(i));}
		this.shapes = newShapes;
		this.repaint();
	}
	public void group() {
		GGroup group = new GGroup();
		this.clipboard.setContents(this.shapes);
		for (int i = this.shapes.size(); i > 0; i--) {
			GShape shape = this.shapes.get(i-1);
			if (shape.isSelected()) {
				shape.setSelected(false);
				group.addShape(shape);
				this.shapes.remove(shape);
			}
		}
		group.setSelected(true);
		this.shapes.add(group);
		this.repaint();
	}
	public void ungroup() {
		this.clipboard.setContents(this.shapes);
		Vector<GShape> tempShapes = new Vector<GShape>();
		for (int i = this.shapes.size(); i > 0; i--) {
			GShape shape = this.shapes.get(i - 1);
			if (shape instanceof GGroup && shape.isSelected()) {
				for (GShape gShape : ((GGroup) shape).getgShapes()) {
					gShape.setSelected(true);
					tempShapes.add(gShape);
				}
				this.shapes.remove(shape);
			}
		}
		this.shapes.addAll(tempShapes);
		repaint();
	}
	public void removeShape() {
		for (int i = 0; i < this.shapes.size(); i++) {
			if (this.shapes.get(i).isSelected()) {
				this.shapes.remove(i);
				this.repaint();
			}
		}
	}
	public void moveShape(int keyCode) {
		for (GShape shape : this.shapes) {
			if (shape.isSelected()) {
				Graphics2D g2d = (Graphics2D)this.getGraphics();
				shape.move(keyCode,g2d);
				this.repaint();
			}
		}
	}
	private void showPopupMenu(MouseEvent e) {popupMenu.show(this, e.getX(), e.getY());}
	// Mouse ActionHandler
		private class MouseHandler implements MouseMotionListener, MouseListener {
			public void mousePressed(MouseEvent e) {
				
				if (e.getModifiersEx() == InputEvent.BUTTON3_DOWN_MASK) {showPopupMenu(e);}
				else{
					int x = e.getX();
					int y = e.getY();
					if (eDrawingState ==EDrawingState.eIdle) {
						GShape shape = onShape(x, y);
						if (shape == null) {
							// 도형 그리기를 할 경우 shape벡터에 있는 모든 도형의 anchor를 지움.(selected를 false로)
							if (currentTool.getEDrawingStyle() == EDrawingStyle.e2Points) {
								initTransforming(null,x, y);
								eDrawingState = EDrawingState.eDrawing;
								setSelected(null);
								repaint();
							}
						} else {
							// transforming를 할 경우 shape벡터에 있는 모든 도형 중 현재의 도형만 빼고 anchor를 지움.(selected를 false로)
							setSelected(shape);
							initTransforming(shape,x, y);
							eDrawingState = EDrawingState.eTransforming;	
						}
					}
				}
			}
			public void mouseDragged(MouseEvent event) {
				int x = event.getX();
				int y = event.getY();
				if (eDrawingState==EDrawingState.eTransforming) {
					if (!(transformer instanceof GResizer)) {repaint();}
					keepTransforming(x, y);
				}else if (eDrawingState==EDrawingState.eDrawing) {
					if (currentTool.getEDrawingStyle() == EDrawingStyle.e2Points) {
						keepTransforming(x, y);
					}
				}
			}
			public void mouseReleased(MouseEvent event) {
				int x = event.getX();
				int y = event.getY();
				if (eDrawingState==EDrawingState.eTransforming) {
					finishTransforming(x, y);
					eDrawingState = EDrawingState.eIdle;
				}else if (eDrawingState==EDrawingState.eDrawing) {
					if (currentTool.getEDrawingStyle() == EDrawingStyle.e2Points) {
						finishTransforming(x, y);
						eDrawingState = EDrawingState.eIdle;
					}
				}
				repaint();
			}
			public void mouseMoved(MouseEvent event) {
				int x = event.getX();
				int y = event.getY();
				if (eDrawingState==EDrawingState.eDrawing) {
					if (currentTool.getEDrawingStyle() == EDrawingStyle.eNPoints) {
						keepTransforming(x, y);
					}
				}else {checkCursor(x, y);}
			}
			public void mouseClicked(MouseEvent event) {
				if (event.getClickCount() == 1) {this.mouse1Clicked(event);}
				else if (event.getClickCount() == 2) {this.mouse2Clicked(event);}
			}
			private void mouse1Clicked(MouseEvent event) {
				int x = event.getX();
				int y = event.getY();
				if (currentTool.getEDrawingStyle() == EDrawingStyle.eNPoints) {
					if (eDrawingState == EDrawingState.eIdle) {
						initTransforming(null,x, y);
						eDrawingState = EDrawingState.eDrawing;
					}else if (eDrawingState ==EDrawingState.eDrawing) {
						continueTransforming(x, y);
					}
				}
			}
			private void mouse2Clicked(MouseEvent event) {
				int x = event.getX();
				int y = event.getY();
				if (currentTool.getEDrawingStyle() == EDrawingStyle.eNPoints && eDrawingState==EDrawingState.eDrawing) {
					finishTransforming(x, y);
					eDrawingState = EDrawingState.eIdle;
				}
			}
			public void mouseEntered(MouseEvent event) {}
			public void mouseExited(MouseEvent event) {}
		}
		
	// ActionHandler
	public class ActionHandler implements ActionListener {
		public void actionPerformed(ActionEvent e) {invokeMethod(e.getActionCommand());}
	}
	public void invokeMethod(String name) {
		try {this.getClass().getMethod(name).invoke(this);}
		catch (Exception e) {e.printStackTrace();}
	}
}
