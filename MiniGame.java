/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package MiniGame;

import java.util.ArrayList;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.Random;
import javax.swing.*;
/**
 * 
 * @author Gorecki
 */
public class MiniGame {
    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            var frame = new ClickFrame();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        });
    }
}
class ClickFrame extends JFrame {
    
    public ClickFrame() {
        add(new MouseComponent());
        pack();
    }
}
class MouseComponent extends JComponent {
    private static final int DEFAULT_WIDTH = 300;
    private static final int DEFAULT_HEIGHT = 300;
    private static final int SIDELENGTH = 10;
    private static final int HPGAP = 3;
    private static final Color colorEnemyMissile = new Color(158, 51, 51);
    private int scores;
    private int halfLength;
    private int limit;
    private int enemyLimit;
    private int maxhp;
    private ArrayList<Square> squares;
    private ArrayList<Square> enemies;
    private ArrayList<Missile> missiles;
    private Square current;
            
    public MouseComponent() {
        squares = new ArrayList<>(16);
        enemies = new ArrayList<>(16);
        missiles = new ArrayList<>(16);
        current = null;
        limit = 5;
        enemyLimit = 30;
        maxhp = 20;
        scores = 0;
        halfLength = SIDELENGTH / 2;
        addMouseListener(new MouseHandler());
        addMouseMotionListener(new MouseMotionHandler());
        Fires fires = new Fires(this);
        Timer timerFires = new Timer(300, fires);
        timerFires.start();
        addEnemy(new Point2D.Double(150, 150));
        Waves waves = new Waves(this);
        Timer timerWaves = new Timer(1600, waves);
        timerWaves.start();
    }
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }
    public void drawSquare(Graphics2D g2, Square s, Color c1, Color c2) {
        int x = (int) s.square.getX();
        int y = (int) s.square.getY() -  HPGAP;
        int x2 = (int) (s.square.getWidth()/(1.0*s.maxhp/s.hp));
        g2.setColor(c1);
        g2.drawLine(x, y, x + x2, y);
        g2.setColor(c2);
        g2.draw(s.square);
    }
    @Override
    public void paintComponent(Graphics g) {
        var g2 = (Graphics2D) g;
        g2.setColor(Color.BLACK);
        g.drawString("Score: " + scores, 200, 10);
        for (Square s : squares) {
            drawSquare(g2, s, Color.GREEN, Color.BLACK);
        }
        for (Square s : enemies) {
            drawSquare(g2, s, Color.GREEN, Color.RED);
        }
        for (Missile m : missiles) {
            double x = m.point.getX();
            double y = m.point.getY();
            if (m.isEnemy()) g2.setColor(colorEnemyMissile);
            else g2.setColor(Color.GRAY);
            g2.drawRect((int) x, (int) y, m.length, m.length);
        }
    }
    public Square find(Point2D p) {
        for (Square r: squares)
            if (r.square.contains(p)) return r;
        return null;
    }
    public void add(Point2D p) {
        if (squares.size() >= limit) return;
        current = new Square(p, halfLength, 2);
        squares.add(current);
        repaint();
    }
    public void remove(Square s) {
        if (s == null) return;
        if (s == current) current = null;
        squares.remove(s);
        repaint();
    }
    private void addEnemy(Point2D p) {
        if (enemies.size() >= enemyLimit) return;
        var enemy = new Square(p, halfLength, maxhp);
        enemies.add(enemy);
        repaint();
    }
    public Square getEnemy(Square s, ArrayList<Square> enemies) {
        if (enemies.isEmpty()) return null;
        Square enemy = null;
        double min = Double.MAX_VALUE;
        double distance;
        for (Square e: enemies) {
            var p2 = e.getCenter();
            var p1 = s.getCenter();
            distance = Math.sqrt(Math.pow(p2.getX()-p1.getX(), 2) + Math.pow(p2.getY()-p1.getY(), 2));
            if (distance < min) {
                min = distance;
                enemy = e;
            }
        }
        return enemy;
    }
    public Square findEnemy(Point2D p, ArrayList<Square> enemies) {
        for (Square r: enemies)
            if (r.square.contains(p)) return r;
        return null;
    }
    public void kill(Square s, ArrayList<Square> enemies) {
        if (this.enemies == enemies) scores += 7;
        else scores -= 1;
        if (scores < 0) scores = 0;
        enemies.remove(s);
    }
    public static double getRad(Point2D p1, Point2D p2) {
        return Math.atan2(p1.getY() - p2.getY(), p1.getX() - p2.getX()) + Math.PI;
    }
    class Missile {
        private Point2D point;
        private double rad;
        private int speed;
        private int length;
        private Timer fireTimer;
        private int lifetime;
        private boolean isEnemy;
        public Missile(Point2D point, double rad, int speed, int length, boolean isEnemy) {
            this.point = new Point2D.Double(point.getX(), point.getY());
            this.rad = rad;
            this.speed = speed;
            this.length = length;
            lifetime = 30;
            this.isEnemy = isEnemy;
        }
        public boolean move() {
            lifetime--;
            if (lifetime >= 0) {
                double nx = point.getX() + Math.cos(rad)*speed;
                double ny = point.getY() + Math.sin(rad)*speed;
                point.setLocation(nx, ny);
                return true;
            }
            else return false;
        }
        public void stopFire() {
            if (fireTimer != null) {
                fireTimer.stop();
                fireTimer = null;
            }
        }
        public int length() { return length; }
        public boolean isEnemy() { return isEnemy; }
        private void startFire(ActionListener fire) {
            fireTimer = new Timer(100, fire);
            fireTimer.start();
        }
    }
    class Fire implements ActionListener {
        private Missile missile;
        private MouseComponent field;
        private ArrayList<Square> enemies;
        public Fire(Missile missile, MouseComponent component, ArrayList<Square> enemies) {
            this.missile = missile;
            this.field = component;
            this.enemies = enemies;
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            if (missile != null) {
                var isLife = missile.move();
                if (!isLife) {
                    missile.stopFire();
                    missiles.remove(missile);
                    missile = null;
                }
                else {
                    var enemy = findEnemy(missile.point, enemies);
                    if (enemy != null) {
                        missile.stopFire();
                        missiles.remove(missile);
                        missile = null;
                        enemy.hp--;
                        if (enemy.hp <= 0) field.kill(enemy, enemies);
                    }
                }
                field.repaint();
            }
        }
    }
    class Waves implements ActionListener {
        private MouseComponent field;
        private Random random;
        public Waves(MouseComponent field) {
            this.field = field;
            random = new Random();
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            field.addEnemy(new Point2D.Double(5+random.nextInt(250), 5+random.nextInt(250)));
        }
    }
    class Fires implements ActionListener {
        private MouseComponent field;
        public Fires(MouseComponent field) {
            this.field = field;
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            for (Square s : squares) {
                var enemy = field.getEnemy(s, enemies);
                if (enemy != null) {
                    Missile m = new Missile(s.getCenter(), getRad(s.getCenter(), enemy.getCenter()), 5, 4, false);
                    missiles.add(m);
                    m.startFire(new Fire(m, field, enemies));
                    field.repaint();
                }
            }
            for (Square s : enemies) {
                var enemy = field.getEnemy(s, squares);
                if (enemy != null) {
                    Missile m = new Missile(s.getCenter(), getRad(s.getCenter(), enemy.getCenter()), 5, 4, true);
                    missiles.add(m);
                    m.startFire(new Fire(m, field, squares));
                    field.repaint();
                }
            }
        }
    }
    private class Square {
        public Rectangle2D square;
        public int length;
        public int hp;
        public int maxhp;
        public Square(Point2D p, int halfLength, int maxhp) {
            double x = p.getX();
            double y = p.getY();
            length = halfLength*2;
            square = new Rectangle2D.Double(x - halfLength, y - halfLength, length, length);
            this.hp = maxhp;
            this.maxhp = maxhp;
        }
        public Point2D getCenter() {
            return new Point2D.Double(square.getCenterX(), square.getCenterY());
        }
    }
    private class MouseHandler extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            current = find(e.getPoint());
            if (current == null) add(e.getPoint());
        }
        @Override
        public void mouseClicked(MouseEvent e) {
            current = find(e.getPoint());
            if (current != null && e.getClickCount() >= 2) remove(current);
        }
    }
    private class MouseMotionHandler implements MouseMotionListener {
        @Override
        public void mouseDragged(MouseEvent e) {
            if (current != null) {
                int x = e.getX();
                int y = e.getY();
                current.square.setFrame(x - halfLength, y - halfLength, SIDELENGTH, SIDELENGTH);
                repaint();
            }
        }
        @Override
        public void mouseMoved(MouseEvent e) {}
        
    }
}