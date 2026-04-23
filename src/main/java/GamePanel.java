import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;

import javax.swing.*;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Arrays;
import java.net.URISyntaxException;
import java.awt.image.*;
import javax.imageio.ImageIO;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
  
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


public class GamePanel extends JPanel implements Runnable
{
private static final int PWIDTH = 1000;
private static final int PHEIGHT = 800;
private Thread animator;
private boolean running = false;
private boolean gameOver = false; 

private BufferedImage dbImage;
private Graphics2D dbg;


int FPS,SFPS;
int fpscount;

Random rnd = new Random();

BufferedImage imagemcharsets;
BufferedImage fundo;

boolean LEFT, RIGHT,UP,DOWN;

int MouseX,MouseY;


float sqX1 = 0;
float sqY1 = 100;

float sqX2 = 0;
float sqY2 = 200;

HashMap<Long,GNodo> mapaNodos = new HashMap<>();
ArrayList<GPath> listaPaths = new ArrayList<>();

double minLat = Double.MAX_VALUE;
double minLon = Double.MAX_VALUE;
double maxLat = Double.NEGATIVE_INFINITY;
double maxLon = Double.NEGATIVE_INFINITY;

double zoom = 0.02;
double translateX = -39906.85500000017;
double translateY = -6695.015000000035;

GNodo selectedNodoA = null;
GNodo selectedNodoB = null;
ArrayList<GNodo> caminhoAStar = new ArrayList<>();
double distanciaTotalMetros = 0.0;
int nodosExpandidosAStar = 0;
double tempoCalculoMs = 0.0;
String statusRota = "Selecione o ponto A";
String nomeArquivoMapa = "sanvicente.osm";
String nomeHeuristica = "Distancia Euclidiana";
private static final File APP_BASE_DIR = descobrirDiretorioBase();
private static final int BOTAO_MAPA_X = 760;
private static final int BOTAO_MAPA_Y = 12;
private static final int BOTAO_MAPA_W = 220;
private static final int BOTAO_MAPA_H = 30;
private static final int PAN_STEP = 180;
private static final double ZOOM_IN_FACTOR = 1.1;
private static final double ZOOM_OUT_FACTOR = 0.9;

boolean arrastandoMapa = false;
boolean houveArrasto = false;
int ultimoMouseX = 0;
int ultimoMouseY = 0;

Rectangle btnUp = new Rectangle(0, 0, 44, 32);
Rectangle btnDown = new Rectangle(0, 0, 44, 32);
Rectangle btnLeft = new Rectangle(0, 0, 44, 32);
Rectangle btnRight = new Rectangle(0, 0, 44, 32);
Rectangle btnZoomIn = new Rectangle(0, 0, 52, 32);
Rectangle btnZoomOut = new Rectangle(0, 0, 52, 32);
String botaoAtivo = "";
long botaoAtivoAteMs = 0;
private final Object mapaLock = new Object();

public GamePanel()
{
	this("sanvicente.osm");
}

public GamePanel(String arquivoMapa)
{
	if (arquivoMapa != null && !arquivoMapa.trim().isEmpty()) {
		nomeArquivoMapa = arquivoMapa.trim();
	}

	setBackground(Color.white);
	setPreferredSize( new Dimension(PWIDTH, PHEIGHT));

	// create game components
	setFocusable(true);

	requestFocus(); // JPanel now receives key events
	
	if (dbImage == null){
		dbImage = new BufferedImage(PWIDTH, PHEIGHT,BufferedImage.TYPE_4BYTE_ABGR);
		if (dbImage == null) {
			System.out.println("dbImage is null");
			return;
		}else{
			dbg = (Graphics2D)dbImage.getGraphics();
		}
	}	
	
	
	// Adiciona um Key Listner
	addKeyListener( new KeyAdapter() {
		public void keyPressed(KeyEvent e)
			{ 
				int keyCode = e.getKeyCode();
				
				if(keyCode == KeyEvent.VK_LEFT){
					LEFT = true;
					moverMapa(PAN_STEP, 0);
				}
				if(keyCode == KeyEvent.VK_RIGHT){
					RIGHT = true;
					moverMapa(-PAN_STEP, 0);
				}
				if(keyCode == KeyEvent.VK_UP){
					UP = true;
					moverMapa(0, PAN_STEP);
				}
				if(keyCode == KeyEvent.VK_DOWN){
					DOWN = true;
					moverMapa(0, -PAN_STEP);
				}
				if(keyCode == KeyEvent.VK_C){
					limparRota();
					statusRota = "Rota limpa. Selecione o ponto A";
				}
				if(keyCode == KeyEvent.VK_M){
					trocarMapaPeloDialogo();
				}
				
				System.out.println(""+translateX+" "+translateY);
			}
		@Override
			public void keyReleased(KeyEvent e ) {
				int keyCode = e.getKeyCode();
				
				if(keyCode == KeyEvent.VK_LEFT){
					LEFT = false;
				}
				if(keyCode == KeyEvent.VK_RIGHT){
					RIGHT = false;
				}
				if(keyCode == KeyEvent.VK_UP){
					UP = false;
				}
				if(keyCode == KeyEvent.VK_DOWN){
					DOWN = false;
				}
			}
	});
	
	addMouseMotionListener(new MouseMotionListener() {
		
		@Override
		public void mouseMoved(MouseEvent e) {
			// TODO Auto-generated method stub
			MouseX = e.getX();
			MouseY = e.getY();
		}
		
		@Override
		public void mouseDragged(MouseEvent e) {
			// Arrasto estilo mapa (pan).
			MouseX = e.getX();
			MouseY = e.getY();
			if (arrastandoMapa) {
				int dx = e.getX() - ultimoMouseX;
				int dy = e.getY() - ultimoMouseY;
				if (Math.abs(dx) > 1 || Math.abs(dy) > 1) {
					houveArrasto = true;
				}
				translateX += dx / zoom;
				translateY += dy / zoom;
				ultimoMouseX = e.getX();
				ultimoMouseY = e.getY();
			}
		}
	});
	
	addMouseListener(new MouseListener() {
		
		@Override
		public void mouseReleased(MouseEvent e) {
			if (arrastandoMapa) {
				arrastandoMapa = false;
				if (!houveArrasto) {
					processarSelecaoNoMapa(e.getX(), e.getY());
				}
			}
		}
		
		@Override
		public void mousePressed(MouseEvent e) {
			if (clicouNoBotaoTrocarMapa(e.getX(), e.getY())) {
				trocarMapaPeloDialogo();
				return;
			}
			if (clicouEmBotaoControle(e.getX(), e.getY())) {
				return;
			}

			if (e.getButton() == MouseEvent.BUTTON1) {
				arrastandoMapa = true;
				houveArrasto = false;
				ultimoMouseX = e.getX();
				ultimoMouseY = e.getY();
			}
		}
		
		@Override
		public void mouseExited(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void mouseEntered(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void mouseClicked(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}
	});
	
	addMouseWheelListener(new MouseWheelListener() {
		
		@Override
		public void mouseWheelMoved(MouseWheelEvent e) {
			// CTRL + roda = zoom fino estilo mapas.
			if (e.isControlDown()) {
				if(e.getWheelRotation()>0) {
					aplicarZoom(ZOOM_OUT_FACTOR);
				}else if(e.getWheelRotation()<0) {
					aplicarZoom(ZOOM_IN_FACTOR);
				}
			} else {
				// Mantem compatibilidade: roda sem CTRL tambem faz zoom.
				if(e.getWheelRotation()>0) {
					aplicarZoom(ZOOM_OUT_FACTOR);
				}else if(e.getWheelRotation()<0) {
					aplicarZoom(ZOOM_IN_FACTOR);
				}
			}
		}
	});
	
	
	try {
		fundo = ImageIO.read( getClass().getResource("fundo.jpg") );
	}
	catch(IOException e) {
		System.out.println("Load Image error:");
	}
	
	MouseX = MouseY = 0;
	
	parseDocument();

} // end of GamePanel()

public void addNotify()
{
	super.addNotify(); // creates the peer
	startGame(); // start the thread
}

private void startGame()
// initialise and start the thread
{
	if (animator == null || !running) {
		animator = new Thread(this);
		animator.start();
	}
} // end of startGame()

public void stopGame()
// called by the user to stop execution
{ running = false; }


public void run()
/* Repeatedly update, render, sleep */
{
	running = true;
	
	long DifTime,TempoAnterior;
	
	int segundo = 0;
	DifTime = 0;
	TempoAnterior = System.currentTimeMillis();
	
	while(running) {
	
		gameUpdate(DifTime); // game state is updated
		gameRender(); // render to a buffer
		paintImmediately(0, 0, PWIDTH, PHEIGHT); // paint with the buffer
	
		try {
			Thread.sleep(1); // sleep a bit
		}	
		catch(InterruptedException ex){}
		
		DifTime = System.currentTimeMillis() - TempoAnterior;
		TempoAnterior = System.currentTimeMillis();
		
		if(segundo!=((int)(TempoAnterior/1000))){
			FPS = SFPS;
			SFPS = 1;
			segundo = ((int)(TempoAnterior/1000));
		}else{
			SFPS++;
		}
	
	}
System.exit(0); // so enclosing JFrame/JApplet exits
} // end of run()

public void parseDocument() {
	carregarMapaInterno(nomeArquivoMapa);
}

private void centralizarMapaComZoomInicial() {
	double larguraMapa = (maxLon - minLon) * 100000.0;
	double alturaMapa = (maxLat - minLat) * 100000.0;
	if (larguraMapa <= 0 || alturaMapa <= 0) {
		return;
	}

	double zoomX = PWIDTH / larguraMapa;
	double zoomY = PHEIGHT / alturaMapa;
	double fitZoom = Math.min(zoomX, zoomY) * 0.90; // pequena margem visual
	zoom = Math.max(0.02, Math.min(fitZoom, 8.0));

	double cx = larguraMapa / 2.0;
	double cy = alturaMapa / 2.0;
	translateX = (PWIDTH / 2.0) - cx;
	translateY = (PHEIGHT / 2.0) - cy;
}

public ArrayList<GNodo> calcularAStar(GNodo origem, GNodo destino) {
	ArrayList<GNodo> caminho = new ArrayList<>();
	if (origem == null || destino == null) {
		return caminho;
	}
	if (origem == destino) {
		caminho.add(origem);
		return caminho;
	}

	HashMap<Long, Float> gScore = new HashMap<>();
	HashMap<Long, Float> fScore = new HashMap<>();
	HashMap<Long, GNodo> cameFrom = new HashMap<>();
	HashMap<Long, Boolean> fechado = new HashMap<>();

	PriorityQueue<GNodo> aberto = new PriorityQueue<>((a, b) -> {
		float fa = fScore.getOrDefault(a.id, Float.MAX_VALUE);
		float fb = fScore.getOrDefault(b.id, Float.MAX_VALUE);
		return Float.compare(fa, fb);
	});

	gScore.put(origem.id, 0f);
	fScore.put(origem.id, heuristica(origem, destino));
	aberto.add(origem);
	nodosExpandidosAStar = 0;

	while (!aberto.isEmpty()) {
		GNodo atual = aberto.poll();
		if (fechado.getOrDefault(atual.id, false)) {
			continue;
		}

		if (atual == destino) {
			LinkedList<GNodo> inverso = new LinkedList<>();
			GNodo cursor = destino;
			inverso.addFirst(cursor);
			while (cameFrom.containsKey(cursor.id)) {
				cursor = cameFrom.get(cursor.id);
				inverso.addFirst(cursor);
			}
			caminho.addAll(inverso);
			System.out.println("A* pronto. Nodos no caminho: " + caminho.size());
			return caminho;
		}

		fechado.put(atual.id, true);
		nodosExpandidosAStar++;
		float gAtual = gScore.getOrDefault(atual.id, Float.MAX_VALUE);
		for (int i = 0; i < atual.listaArestas.size(); i++) {
			Aresta ar = atual.listaArestas.get(i);
			GNodo vizinho = (ar.A == atual) ? ar.B : ar.A;

			if (fechado.getOrDefault(vizinho.id, false)) {
				continue;
			}

			float tentativo = gAtual + ar.size;
			float gVizinho = gScore.getOrDefault(vizinho.id, Float.MAX_VALUE);
			if (tentativo < gVizinho) {
				cameFrom.put(vizinho.id, atual);
				gScore.put(vizinho.id, tentativo);
				fScore.put(vizinho.id, tentativo + heuristica(vizinho, destino));
				aberto.add(vizinho);
			}
		}
	}

	System.out.println("A* nao encontrou rota entre os pontos selecionados.");
	return caminho;
}

private float heuristica(GNodo a, GNodo b) {
	double dlat = a.lat - b.lat;
	double dlon = a.lon - b.lon;
	return (float)(Math.sqrt(dlat * dlat + dlon * dlon) * 1000000.0);
}

int timerfps = 0;

private void gameUpdate(long DiffTime)
{
	sqX1+=0.5f;
	
	sqX2 = sqX2 + 100*DiffTime/1000.0f;
}

private void gameRender()
// draw the current frame to an image buffer
{
	dbg.setColor(Color.white);
	dbg.fillRect(0, 0, PWIDTH, PHEIGHT);
	dbg.setColor(Color.BLUE);
	dbg.drawString("FPS: "+FPS, 20, 20);
	
	AffineTransform trans = dbg.getTransform();
	
	
	
	dbg.translate(PWIDTH/2, PHEIGHT/2);
	
	dbg.scale(zoom, zoom);
	
	dbg.translate(-PWIDTH/2, -PHEIGHT/2);
	
	dbg.translate(translateX, translateY);
	
	synchronized (mapaLock) {
		for(int i = 0; i < listaPaths.size();i++) {
			GPath gpath = listaPaths.get(i);
			GNodo gnA = mapaNodos.get(gpath.idnodos.get(0));
			if (gnA == null) {
				continue;
			}
			for(int j = 1; j < gpath.idnodos.size();j++) {
				GNodo gnB = mapaNodos.get(gpath.idnodos.get(j));
				if (gnB == null) {
					continue;
				}
				int x1 = (int)(gnA.lon*100000-minLon*100000);
				int y1 = (int)(minLat*100000-gnA.lat*100000);
				int x2 = (int)(gnB.lon*100000-minLon*100000);
				int y2 = (int)(minLat*100000-gnB.lat*100000);
				gnA = gnB;
			
//			if(x1<0&&x2<0) {
//				continue;
//			}
//			if(y1<0&&y2<0) {
//				continue;
//			}
//			if(x1>1000&&x2>1000) {
//				continue;
//			}
//			if(y1>1000&&y2>1000) {
//				continue;
//			}
			
				dbg.drawLine(x1,y1,x2,y2);
				
			}
		}

		if (caminhoAStar.size() > 1) {
			dbg.setColor(Color.MAGENTA);
			for (int i = 0; i < caminhoAStar.size() - 1; i++) {
				GNodo gnA = caminhoAStar.get(i);
				GNodo gnB = caminhoAStar.get(i + 1);
				int x1 = (int)(gnA.lon * 100000 - minLon * 100000);
				int y1 = (int)(minLat * 100000 - gnA.lat * 100000);
				int x2 = (int)(gnB.lon * 100000 - minLon * 100000);
				int y2 = (int)(minLat * 100000 - gnB.lat * 100000);
				dbg.drawLine(x1, y1, x2, y2);
			}
		}
		dbg.setColor(Color.red);
		int skip = 1;
		if (zoom < 0.03) {
			skip = 10;
		} else if (zoom < 0.06) {
			skip = 5;
		} else if (zoom < 0.1) {
			skip = 2;
		}
		int idx = 0;
		for (Iterator iterator = mapaNodos.keySet().iterator(); iterator.hasNext();) {
			Long key = (Long) iterator.next();
			idx++;
			if (skip > 1 && (idx % skip != 0)) {
				continue;
			}
			GNodo gnB = mapaNodos.get(key);
			int x = (int)(gnB.lon*100000-minLon*100000);
			int y = (int)(minLat*100000-gnB.lat*100000);
			if(gnB==selectedNodoA) {
				dbg.setColor(Color.green);
				dbg.fillRect(x-20, y-20, 40,40);
			}else if(gnB==selectedNodoB) {
				dbg.setColor(Color.ORANGE);
				dbg.fillRect(x-20, y-20, 40,40);			
			}else{
				dbg.setColor(Color.red);
				dbg.fillRect(x-1, y-1, 3,3);
			}
		}
	} 
	
	dbg.setTransform(trans);
	desenharPainelInfo(dbg);
	desenharBotaoTrocarMapa(dbg);
	desenharBotoesControle(dbg);
}

private void desenharPainelInfo(Graphics2D dbg) {
	int painelX = 12;
	int painelY = 35;
	int painelW = 560;
	int painelH = 230;

	dbg.setColor(new Color(255, 255, 255, 220));
	dbg.fillRoundRect(painelX, painelY, painelW, painelH, 12, 12);
	dbg.setColor(new Color(60, 60, 60));
	dbg.drawRoundRect(painelX, painelY, painelW, painelH, 12, 12);

	int y = painelY + 22;
	dbg.setColor(Color.BLACK);
	dbg.drawString("A* RoadGraph - " + nomeArquivoMapa, painelX + 12, y);
	y += 20;
	dbg.drawString("Status: " + statusRota, painelX + 12, y);
	y += 20;
	dbg.drawString("Nos no caminho: " + caminhoAStar.size() + " | Nos expandidos: " + nodosExpandidosAStar, painelX + 12, y);
	y += 20;
	dbg.drawString(String.format("Distancia estimada da rota: %.2f km", distanciaTotalMetros / 1000.0), painelX + 12, y);
	y += 20;
	dbg.drawString(String.format("Tempo do A*: %.2f ms", tempoCalculoMs), painelX + 12, y);
	y += 20;
	dbg.drawString("Heuristica: " + nomeHeuristica, painelX + 12, y);
	y += 20;
	dbg.drawString("A: " + formatarNodo(selectedNodoA), painelX + 12, y);
	y += 20;
	dbg.drawString("B: " + formatarNodo(selectedNodoB), painelX + 12, y);
	y += 20;

	desenharLegendaCor(dbg, painelX + 12, y, Color.GREEN, "Ponto A");
	desenharLegendaCor(dbg, painelX + 120, y, Color.ORANGE, "Ponto B");
	desenharLegendaCor(dbg, painelX + 220, y, Color.MAGENTA, "Rota A*");
	dbg.setColor(Color.DARK_GRAY);
	dbg.drawString("Teclas: C limpar | M trocar mapa", painelX + 330, y + 10);
	y += 22;
	dbg.drawString("Navegacao: setas ou arraste com mouse (botao esquerdo)", painelX + 12, y);
	y += 18;
	dbg.drawString("Zoom: roda do mouse ou CTRL + roda | Botoes + e - na tela", painelX + 12, y);
}

private void desenharBotaoTrocarMapa(Graphics2D dbg) {
	dbg.setColor(new Color(240, 240, 240, 230));
	dbg.fillRoundRect(BOTAO_MAPA_X, BOTAO_MAPA_Y, BOTAO_MAPA_W, BOTAO_MAPA_H, 12, 12);
	dbg.setColor(new Color(70, 70, 70));
	dbg.drawRoundRect(BOTAO_MAPA_X, BOTAO_MAPA_Y, BOTAO_MAPA_W, BOTAO_MAPA_H, 12, 12);
	dbg.drawString("Trocar mapa", BOTAO_MAPA_X + 16, BOTAO_MAPA_Y + 20);
	dbg.drawString("(ou tecla M)", BOTAO_MAPA_X + 110, BOTAO_MAPA_Y + 20);
}

private boolean clicouNoBotaoTrocarMapa(int x, int y) {
	return x >= BOTAO_MAPA_X && x <= BOTAO_MAPA_X + BOTAO_MAPA_W
			&& y >= BOTAO_MAPA_Y && y <= BOTAO_MAPA_Y + BOTAO_MAPA_H;
}

private void desenharBotoesControle(Graphics2D dbg) {
	atualizarLayoutControles();
	desenharBotao(dbg, btnUp, "↑");
	desenharBotao(dbg, btnDown, "↓");
	desenharBotao(dbg, btnLeft, "←");
	desenharBotao(dbg, btnRight, "→");
	desenharBotao(dbg, btnZoomIn, "+");
	desenharBotao(dbg, btnZoomOut, "-");
}

private void desenharBotao(Graphics2D dbg, Rectangle r, String texto) {
	boolean ativo = texto.equals(botaoAtivo) && System.currentTimeMillis() <= botaoAtivoAteMs;
	if (ativo) {
		dbg.setColor(new Color(200, 225, 255, 235));
	} else {
		dbg.setColor(new Color(245, 245, 245, 230));
	}
	dbg.fillRoundRect(r.x, r.y, r.width, r.height, 10, 10);
	dbg.setColor(new Color(60, 60, 60));
	dbg.drawRoundRect(r.x, r.y, r.width, r.height, 10, 10);
	dbg.drawString(texto, r.x + (r.width / 2) - 4, r.y + (r.height / 2) + 5);
}

private boolean clicouEmBotaoControle(int x, int y) {
	atualizarLayoutControles();
	if (btnUp.contains(x, y)) {
		ativarEfeitoBotao("↑");
		moverMapa(0, PAN_STEP);
		return true;
	}
	if (btnDown.contains(x, y)) {
		ativarEfeitoBotao("↓");
		moverMapa(0, -PAN_STEP);
		return true;
	}
	if (btnLeft.contains(x, y)) {
		ativarEfeitoBotao("←");
		moverMapa(PAN_STEP, 0);
		return true;
	}
	if (btnRight.contains(x, y)) {
		ativarEfeitoBotao("→");
		moverMapa(-PAN_STEP, 0);
		return true;
	}
	if (btnZoomIn.contains(x, y)) {
		ativarEfeitoBotao("+");
		aplicarZoom(ZOOM_IN_FACTOR);
		return true;
	}
	if (btnZoomOut.contains(x, y)) {
		ativarEfeitoBotao("-");
		aplicarZoom(ZOOM_OUT_FACTOR);
		return true;
	}
	return false;
}

private void ativarEfeitoBotao(String id) {
	botaoAtivo = id;
	botaoAtivoAteMs = System.currentTimeMillis() + 180;
}

private void moverMapa(double dx, double dy) {
	translateX += dx;
	translateY += dy;
}

private void aplicarZoom(double fator) {
	zoom = zoom * fator;
	if (zoom < 0.01) {
		zoom = 0.01;
	}
	if (zoom > 20.0) {
		zoom = 20.0;
	}
}

private void processarSelecaoNoMapa(int mouseX, int mouseY) {
	int PW2 = PWIDTH/2;
	int PH2 = PHEIGHT/2;
	double lon = ((mouseX-PW2)/zoom+PW2-translateX+minLon*100000)/100000;
	double lat = (minLat*100000-((mouseY-PH2)/zoom+PH2-translateY))/100000;

	if(selectedNodoA==null) {
		selectedNodoA = nodoMaisProximo(lat, lon);
		selectedNodoB = null;
		caminhoAStar.clear();
		distanciaTotalMetros = 0.0;
		nodosExpandidosAStar = 0;
		tempoCalculoMs = 0.0;
		statusRota = "Ponto A selecionado. Agora selecione o ponto B";
	}else if(selectedNodoB==null) {
		selectedNodoB = nodoMaisProximo(lat, lon);
		long inicioCalculo = System.nanoTime();
		caminhoAStar = calcularAStar(selectedNodoA, selectedNodoB);
		tempoCalculoMs = (System.nanoTime() - inicioCalculo) / 1000000.0;
		distanciaTotalMetros = calcularDistanciaTotalMetros(caminhoAStar);
		if (caminhoAStar.isEmpty()) {
			statusRota = "Nao foi encontrada rota entre A e B";
		} else {
			statusRota = "Rota calculada com sucesso";
		}
	}else {
		selectedNodoA = nodoMaisProximo(lat, lon);
		selectedNodoB=null;
		caminhoAStar.clear();
		distanciaTotalMetros = 0.0;
		nodosExpandidosAStar = 0;
		tempoCalculoMs = 0.0;
		statusRota = "Novo ponto A selecionado. Escolha o ponto B";
	}
}

private void desenharLegendaCor(Graphics2D dbg, int x, int y, Color cor, String texto) {
	dbg.setColor(cor);
	dbg.fillRect(x, y - 10, 14, 14);
	dbg.setColor(Color.BLACK);
	dbg.drawRect(x, y - 10, 14, 14);
	dbg.drawString(texto, x + 20, y + 2);
}

private String formatarNodo(GNodo nodo) {
	if (nodo == null) {
		return "--";
	}
	return String.format("lat %.5f, lon %.5f", nodo.lat, nodo.lon);
}

private double calcularDistanciaTotalMetros(ArrayList<GNodo> caminho) {
	double total = 0.0;
	for (int i = 0; i < caminho.size() - 1; i++) {
		total += distanciaHaversineMetros(caminho.get(i), caminho.get(i + 1));
	}
	return total;
}

private double distanciaHaversineMetros(GNodo a, GNodo b) {
	double raioTerra = 6371000.0;
	double lat1 = Math.toRadians(a.lat);
	double lat2 = Math.toRadians(b.lat);
	double dLat = Math.toRadians(b.lat - a.lat);
	double dLon = Math.toRadians(b.lon - a.lon);

	double x = Math.sin(dLat / 2.0) * Math.sin(dLat / 2.0)
			+ Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2.0) * Math.sin(dLon / 2.0);
	double c = 2.0 * Math.atan2(Math.sqrt(x), Math.sqrt(1.0 - x));
	return raioTerra * c;
}

private void limparRota() {
	selectedNodoA = null;
	selectedNodoB = null;
	caminhoAStar.clear();
	distanciaTotalMetros = 0.0;
	nodosExpandidosAStar = 0;
	tempoCalculoMs = 0.0;
}


public void paintComponent(Graphics g)
{
	super.paintComponent(g);
	if (dbImage != null)
		g.drawImage(dbImage, 0, 0, null);
}

public GNodo nodoMaisProximo(double lat,double lon) {
	GNodo nodoselecionado = null;
	double menorvalor = 100000;
	
	synchronized (mapaLock) {
		for (Iterator iterator = mapaNodos.keySet().iterator(); iterator.hasNext();) {
			Long nodekey = (Long) iterator.next();
			GNodo onodo = mapaNodos.get(nodekey);
			double dist = distNodo(onodo, lat, lon);
			if(nodoselecionado==null||dist<menorvalor) {
				menorvalor = dist;
				nodoselecionado = onodo;
			}
		}
	}
	
	return nodoselecionado;
}

public static double distNodo(GNodo onodo,double lat,double lon) {
	double dlat = onodo.lat-lat;
	double dlon = onodo.lon-lon;
	return Math.sqrt(dlat*dlat+dlon*dlon);
}


public static void main(String args[])
{
	String arquivoMapa = "sanvicente.osm";
	if (args != null && args.length > 0 && args[0] != null && !args[0].trim().isEmpty()) {
		arquivoMapa = args[0].trim();
	} else {
		String escolhido = escolherMapaEmDialogo();
		if (escolhido != null && !escolhido.trim().isEmpty()) {
			arquivoMapa = escolhido;
		}
	}
	GamePanel ttPanel = new GamePanel(arquivoMapa);

  // create a JFrame to hold the timer test JPanel
  JFrame app = new JFrame("RoadGraph A* - " + arquivoMapa);
  app.getContentPane().add(ttPanel, BorderLayout.CENTER);
  app.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

  app.pack();
  app.setResizable(false);  
  app.setVisible(true);
} // end of main()

private static String escolherMapaEmDialogo() {
	File[] arquivos = APP_BASE_DIR.listFiles((dir, nome) -> nome.toLowerCase().endsWith(".osm"));
	if (arquivos == null || arquivos.length == 0) {
		return "sanvicente.osm";
	}
	Arrays.sort(arquivos, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));

	String[] opcoes = new String[arquivos.length];
	for (int i = 0; i < arquivos.length; i++) {
		opcoes[i] = arquivos[i].getName();
	}
	Object selecionado = JOptionPane.showInputDialog(
			null,
			"Escolha o mapa OSM para abrir:",
			"RoadGraph A*",
			JOptionPane.QUESTION_MESSAGE,
			null,
			opcoes,
			opcoes[0]);

	if (selecionado == null) {
		return opcoes[0];
	}
	return selecionado.toString();
}

private void trocarMapaPeloDialogo() {
	String escolhido = escolherMapaEmDialogo();
	if (escolhido == null || escolhido.trim().isEmpty()) {
		return;
	}
	if (escolhido.equals(nomeArquivoMapa)) {
		statusRota = "Mapa atual mantido: " + nomeArquivoMapa;
		return;
	}

	nomeArquivoMapa = escolhido;
	limparRota();
	statusRota = "Carregando mapa: " + nomeArquivoMapa;
	boolean carregou = carregarMapaInterno(nomeArquivoMapa);
	if (carregou) {
		statusRota = "Mapa carregado: " + nomeArquivoMapa + ". Selecione o ponto A";
	} else {
		statusRota = "Falha ao carregar mapa: " + nomeArquivoMapa;
	}
	Window janela = SwingUtilities.getWindowAncestor(this);
	if (janela instanceof JFrame) {
		((JFrame)janela).setTitle("RoadGraph A* - " + nomeArquivoMapa);
	}
	requestFocusInWindow();
}

private void atualizarLayoutControles() {
	int baseX = PWIDTH - 120;
	int baseY = 78;
	btnUp.setBounds(baseX + 36, baseY, 44, 32);
	btnDown.setBounds(baseX + 36, baseY + 64, 44, 32);
	btnLeft.setBounds(baseX - 8, baseY + 32, 44, 32);
	btnRight.setBounds(baseX + 80, baseY + 32, 44, 32);
	btnZoomIn.setBounds(baseX - 2, baseY + 108, 52, 32);
	btnZoomOut.setBounds(baseX + 62, baseY + 108, 52, 32);
}

private boolean carregarMapaInterno(String arquivoMapa) {
	HashMap<Long,GNodo> novosMapaNodos = new HashMap<>();
	ArrayList<GPath> novaListaPaths = new ArrayList<>();
	double novoMinLat = Double.MAX_VALUE;
	double novoMinLon = Double.MAX_VALUE;
	double novoMaxLat = Double.NEGATIVE_INFINITY;
	double novoMaxLon = Double.NEGATIVE_INFINITY;

	try {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		
		File file = new File(APP_BASE_DIR, arquivoMapa);
		if (!file.exists()) {
			file = new File(APP_BASE_DIR, "sanvicente.osm");
			arquivoMapa = "sanvicente.osm";
		}
		if (!file.exists()) {
			JOptionPane.showMessageDialog(
					null,
					"Nenhum mapa .osm foi encontrado em:\n" + APP_BASE_DIR.getAbsolutePath(),
					"RoadGraph A*",
					JOptionPane.ERROR_MESSAGE);
			return false;
		}
		
		Document doc = builder.parse(file);
		NodeList nodeList = doc.getDocumentElement().getChildNodes();
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			if(node.getNodeName().equals("node")) {
				NamedNodeMap map = node.getAttributes();
				GNodo gnodo = new GNodo();
				gnodo.id = Long.parseLong(map.getNamedItem("id").getNodeValue());
				gnodo.lat = Double.parseDouble(map.getNamedItem("lat").getNodeValue());
				gnodo.lon = Double.parseDouble(map.getNamedItem("lon").getNodeValue());
				novosMapaNodos.put(gnodo.id, gnodo);
				if(gnodo.lat<novoMinLat) novoMinLat = gnodo.lat;
				if(gnodo.lon<novoMinLon) novoMinLon = gnodo.lon;
				if(gnodo.lon>novoMaxLon) novoMaxLon = gnodo.lon;
				if(gnodo.lat>novoMaxLat) novoMaxLat = gnodo.lat;
			}
			if(node.getNodeName().equals("way")) {
				GPath gpath = new GPath();
				NodeList subnodeList = node.getChildNodes();
				boolean descartar = true;
				for (int j = 0; j < subnodeList.getLength(); j++) {
					Node subnode = subnodeList.item(j);
					if(subnode.getNodeName().equals("nd")) {
						long ref = Long.parseLong(subnode.getAttributes().getNamedItem("ref").getNodeValue());
						gpath.idnodos.add(ref);
					}
					if(subnode.getNodeName().equals("tag")) {
						String sn = subnode.getAttributes().getNamedItem("k").getNodeValue();
						if(sn.equals("highway")) {
							descartar = false;
						}
					}
				}
				if(!descartar) {
					novaListaPaths.add(gpath);
					for(int z = 0; z < gpath.idnodos.size();z++) {
						GNodo gnB = novosMapaNodos.get(gpath.idnodos.get(z));
						if (gnB != null) {
							gnB.ativo = true;
						}
					}
				}
			}
		}
		
		for (Iterator iterator = novosMapaNodos.keySet().iterator(); iterator.hasNext();) {
			Long key = (Long) iterator.next();
			GNodo gnB = novosMapaNodos.get(key);
			if(!gnB.ativo) {
				iterator.remove();
			}
		}
		
		for(int i = 0; i < novaListaPaths.size();i++) {
			GPath gpath = novaListaPaths.get(i);
			GNodo gnA = novosMapaNodos.get(gpath.idnodos.get(0));
			if (gnA == null) continue;
			for(int j = 1; j < gpath.idnodos.size();j++) {
				GNodo gnB = novosMapaNodos.get(gpath.idnodos.get(j));
				if (gnB == null) continue;
				Aresta ar = new Aresta(gnA, gnB);
				gnA.listaArestas.add(ar);
				gnB.listaArestas.add(ar);
				gnA = gnB;
			}
		}
		
		synchronized (mapaLock) {
			mapaNodos.clear();
			mapaNodos.putAll(novosMapaNodos);
			listaPaths.clear();
			listaPaths.addAll(novaListaPaths);
			minLat = novoMinLat;
			minLon = novoMinLon;
			maxLat = novoMaxLat;
			maxLon = novoMaxLon;
			nomeArquivoMapa = arquivoMapa;
		}
		centralizarMapaComZoomInicial();
		return true;
	} catch (Exception e) {
		JOptionPane.showMessageDialog(
				null,
				"Erro ao carregar mapa:\n" + e.getMessage(),
				"RoadGraph A*",
				JOptionPane.ERROR_MESSAGE);
		return false;
	}
}

private static File descobrirDiretorioBase() {
	try {
		File origem = new File(GamePanel.class.getProtectionDomain().getCodeSource().getLocation().toURI());
		if (origem.isFile()) {
			return origem.getParentFile();
		}
		return origem;
	} catch (URISyntaxException e) {
		return new File(".").getAbsoluteFile();
	}
}

} // end of GamePanel class

