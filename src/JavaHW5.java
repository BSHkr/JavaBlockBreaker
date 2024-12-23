import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

abstract class GameObject {
    int x, y, w, h;

    void draw(Graphics g) {
    }

    void update(float dt, Racket racket) {
    }

    Point collisionResolution(LinkedList<Block> blocks) {
        return null;
    }

    Rectangle getBounds() {
        return new Rectangle(x, y, w, h);
    }
}

class Racket extends GameObject {
    Racket(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    @Override
    void draw(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(Color.pink);
        g2.fillRoundRect(x, y, w, h, 10, 10);
    }

    @Override
    Rectangle getBounds() {
        return super.getBounds();
    }
}

class Wall extends GameObject {
    Wall(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    @Override
    void draw(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        GradientPaint gp2 = new GradientPaint(0, 0, Color.gray, 0, 800, Color.darkGray);

        g2.setPaint(gp2);
        g2.fillRect(x, y, w, h);

        g.setColor(Color.white);
        g.drawRect(x, y, w, h);
    }

    @Override
    Rectangle getBounds() {
        return super.getBounds();
    }
}

class Block extends GameObject {
    boolean isYellow = false;
    boolean isCollision = false;

    Block(Point p, boolean isYellow, int w, int h) {
        x = p.x;
        y = p.y;
        this.isYellow = isYellow;
        this.w = w;
        this.h = h;
    }

    @Override
    void draw(Graphics g) {
        Graphics2D g2 = (Graphics2D) g; // 그라데이션용
        GradientPaint gp2;

        if (isYellow) {
            g.setColor(Color.YELLOW);
            gp2 = new GradientPaint(0, 0, Color.white, 0, 800, Color.YELLOW);
        } else {
            g.setColor(new Color(150, 100, 150));
            gp2 = new GradientPaint(0, 0, Color.white, 0, 800, new Color(150, 100, 150));
        }


        g.fillRect(x, y, w, h);

        g2.setPaint(gp2);
        g2.drawRect(x, y, w, h);

    }

    @Override
    Rectangle getBounds() {
        return super.getBounds();
    }
}

class Ball extends GameObject {
    // 공 x, y 위치와 반지름 값
    int x, y, r;
    int prev_x, prev_y;
    // 공의 속도
    int vx, vy;
    double angle = Math.random() * 3.141592;

    GamePlay play;

    Ball(GamePlay play) { // 첫 생성용
        x = 400;
        y = 700;
        r = 5;
        this.play = play;

        vx = (int) (Math.cos(angle) * 150);
        vy = (int) -(Math.sin(angle) * 150);
    }

    Ball(GamePlay play, int x, int y, int vx, int vy) {
        this.x = x;
        this.y = y;
        this.play = play;
        this.vx = (int) (Math.cos(angle) * vx);
        this.vy = (int) -(Math.sin(angle) * vy);

        r = 5;
    }

    @Override
    Rectangle getBounds() {
        return super.getBounds();
    }


    @Override
    void draw(Graphics g) {
        g.setColor(Color.white);
        g.fillOval(x - r, y - r, 2 * r, 2 * r);
    }

    @Override
    void update(float dt, Racket racket) {
        prev_x = x;
        prev_y = y;

        x += vx * dt;
        y += vy * dt;

        if (prev_y < 20) {
            y = 20;
            vy = -vy;
        }

        if (x < 20) {
            x = 20;
            vx = -vx;
        } else if (x > 780) {
            x = 780;
            vx = -vx;
        }

        // 라켓 충돌 체크 - 더 정확한 충돌 감지
        Rectangle racketBounds = racket.getBounds();
        Rectangle ballBounds = new Rectangle(x - r, y - r, 2 * r, 2 * r);

        if (ballBounds.intersects(racketBounds)) {
            // 라켓의 어느 부분에 맞았는지에 따라 반사각 조절
            float relativeIntersectX = (racket.x + (racket.w / 2)) - x;
            float normalizedRelativeIntersectX = (relativeIntersectX / (racket.w / 2));
            float bounceAngle = (float) (normalizedRelativeIntersectX * (5 * Math.PI / 12)); // 최대 75도

            float speed = (float) Math.sqrt(vx * vx + vy * vy);
            vx = (int) (speed * -Math.sin(bounceAngle));
            vy = (int) (speed * -Math.cos(bounceAngle));

            y = racket.y - r; // 라켓 위로 위치 조정
            new Thread(() -> {
                SoundManager.playSound("BallHitted.wav");
            }).start();
        }
    }

    @Override
    Point collisionResolution(LinkedList<Block> blocks) {
        Point p = new Point(0, 0);

        Rectangle ballBounds = new Rectangle(x - r, y - r, 2 * r, 2 * r);

        for (int i = blocks.size() - 1; i >= 0; i--) {
            Block block = blocks.get(i);
            Rectangle blockBounds = block.getBounds();

            if (ballBounds.intersects(blockBounds)) {
                // 충돌 방향 결정을 위한 겹침 영역 계산
                Rectangle intersection = ballBounds.intersection(blockBounds);

                if (intersection.height < intersection.width) {
                    // 상하 충돌
                    vy = -vy;
                    if (y < block.y) {
                        y = block.y - r;
                    } else {
                        y = block.y + block.h + r;
                    }
                } else {
                    // 좌우 충돌
                    vx = -vx;
                    if (x < block.x) {
                        x = block.x - r;
                    } else {
                        x = block.x + block.w + r;
                    }
                }

                block.isCollision = true;
                play.score += 10;
                if (block.isYellow) {
                    p.x = x;
                    p.y = y;
                }

                if (block.isYellow) {
                    return p;
                }
                break;
            }
        }
        return p;
    }
}

class Title extends GameObject {
    Font font1 = new Font("바탕체", Font.BOLD, 60);
    Font font2 = new Font("stencil", Font.BOLD, 80);
    Font font3 = new Font("바탕체", Font.LAYOUT_LEFT_TO_RIGHT, 25);

    String str1 = "Java Programing";
    String str2 = "Homework #5";
    String str3 = "Block Breaker";
    String str4 = "Press Spacebar to play!";

    @Override
    void draw(Graphics g) {
        g.setFont(font1);
        g.setColor(Color.black);
        g.drawString(str1, 130, 170);
        g.drawString(str2, 190, 230);

        g.setColor(Color.white);
        g.drawString(str1, 127, 170);
        g.drawString(str2, 187, 230);

        g.setFont(font2);
        g.setColor(Color.black);
        g.drawString(str3, 115, 390);
        g.setColor(Color.white);
        g.drawString(str3, 112, 390);

        g.setFont(font3);
        g.setColor(Color.black);
        g.drawString(str4, 253, 600);

        g.setColor(Color.red);
        g.drawString(str4, 250, 600);
    }
}

class GamePlay extends GameObject {
    LinkedList<Block> blocks = new LinkedList<Block>();
    LinkedList<Ball> balls = new LinkedList<Ball>();
    LinkedList<Wall> walls = new LinkedList<Wall>();
    Racket racket;

    // 라켓 정보
    int x = 335, y = 700;
    int xsize = 130, ysize = 25;

    int bx_size, by_size; //블럭 정보

    int stage = 1;
    int score = 0;

    void init() {
        balls.clear();
        blocks.clear();
        walls.clear();

        // 첫 공 추가
        Ball ball = new Ball(this);
        balls.add(ball);

        // 라켓 추가
        racket = new Racket(x, y, xsize, ysize);

        // 기본 벽 추가
        Wall wall1 = new Wall(0, 0, 800, 20);
        walls.add(wall1);
        Wall wall2 = new Wall(0, 20, 20, 780);
        walls.add(wall2);
        Wall wall3 = new Wall(780, 20, 20, 780);
        walls.add(wall3);

        bx_size = (753 - (stage * 3 - 1) * 5) / (stage * 3);
        by_size = (390 - (stage * 3 - 1) * 5) / (stage * 3);

        int cnt = 0; // 노란블럭 생성제한을 위한 cnt값

        for (int i = 0; i < 3 * stage; i++) {
            cnt = 0;
            for (int j = 0; j < 3 * stage; j++) {
                // 블럭 사이의 간격은 5
                Point c = new Point(25 + (bx_size * j) + (5 * j), 25 + (5 * i) + (by_size * i));

                int Yellow = (int) (Math.random() * 2);
                boolean isYellow = false;


                // 원래는 총 갯수만 카운트 하였으나, 그럴경우 윗줄쪽에 노란 블럭이 모여있는 상황이 발생함
                if (cnt == (stage * 3) / 2) // 한줄에 최대 (stage * 3 / 2) 만큼 노란 블럭 생성
                    Yellow = 0;

                if (Yellow == 1) {
                    isYellow = true;
                    cnt++;
                } else
                    isYellow = false;

                Block r = new Block(c, isYellow, bx_size, by_size);
                blocks.add(r);
            }
        }
    }

    @Override
    void draw(Graphics g) {
        racket.draw(g);

        for (var b : blocks)
            b.draw(g);

        for (var b : balls)
            b.draw(g);

        for (var b : walls)
            b.draw(g);
    }

    void changeStage() {
        stage++;
        init();
    }

    void addBall(Ball ball) {
        balls.add(ball);
    }
}

class GameOver extends GameObject {
    int highScore = 0;
    int score = 0;

    Font font1 = new Font("stencil", Font.BOLD, 110);
    Font font2 = new Font("stencil", Font.BOLD, 40);
    Font font3 = new Font("바탕체", Font.LAYOUT_LEFT_TO_RIGHT, 40);

    String str1 = "Game Over";
    String str2 = "High Score: " + highScore;
    String str3 = "Your Score: " + score;
    String str4 = "Press Spacebar!";

    void draw(Graphics g) {
        g.setFont(font1);

        g.setColor(Color.BLACK);
        g.drawString(str1, 70, 310);
        g.setColor(Color.white);
        g.drawString(str1, 67, 310);

        g.setFont(font2);
        g.setColor(Color.BLACK);
        g.drawString(str2, 240, 450);
        g.setColor(Color.white);
        g.drawString(str2, 237, 450);

        g.setColor(Color.BLACK);
        g.drawString(str3, 240, 490);
        g.setColor(Color.white);
        g.drawString(str3, 237, 490);
    }
}

class JavaHW5Panel extends JPanel implements KeyListener, Runnable {
    Title title = new Title();
    GamePlay gamePlay = new GamePlay();
    GameOver gameOver = new GameOver();

    int highScore = 0;
    int flag;
    int mode;

    final static int GAME_START = 1;
    final static int GAME_PLAY = 2;
    final static int GAME_OVER = 3;
    int previousMode;

    JavaHW5Panel() {
        mode = GAME_START;
        flag = 1;

        SoundManager.playBGM("Gamestart.wav");

        Thread t = new Thread(this);
        t.start();

        requestFocus();
        setFocusable(true);
        addKeyListener(this);
    }

    void handleModeChange() {
        if (previousMode != mode) { // 모드가 변경되었을 때만 실행
            SoundManager.stopCurrentSound(); // 이전 사운드 중지

            // 새로운 모드에 따라 사운드 재생
            new Thread(() -> {
                switch (mode) {
                    case GAME_START:
                        break;
                    case GAME_PLAY:
                        break;
                    case GAME_OVER:
                        SoundManager.playSound("Gameover2.wav");
                        break;
                }
            }).start();

            previousMode = mode; // 현재 모드를 이전 모드로 저장
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g; // 그라데이션용

        handleModeChange();

        GradientPaint gp = new GradientPaint(0, 0, Color.BLACK, 0, 800, new Color(119, 119, 156));
        g2.setPaint(gp);
        g2.fillRect(0, 0, 800, 800);

        if (mode == GAME_START) {
            gamePlay.score = 0;
            gamePlay.stage = 1;
            title.draw(g);
        } else if (mode == GAME_PLAY) {
            flag = 1;

            gamePlay.draw(g);

        } else if (mode == GAME_OVER) {
            gameOver.score = gamePlay.score;
            gameOver.str3 = "Your Score: " + gameOver.score;
            if (gameOver.score > highScore) {
                highScore = gameOver.score;
            }

            gameOver.str2 = "High Score: " + highScore;
            gameOver.draw(g);
            g.setFont(gameOver.font3);
            g.setColor(Color.black);

            g.drawString(gameOver.str4, 240, 600);
            g.setColor(Color.red);
            g.drawString(gameOver.str4, 237, 600);
        }

        repaint();
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            if (mode == GAME_START) {
                mode = GAME_PLAY;

                gamePlay.init();
            } else if (mode == GAME_OVER) {
                mode = GAME_START;
            } else if (mode == GAME_PLAY) {
                mode = GAME_OVER;
                gamePlay.init();
            }
        }
        if (mode == GAME_PLAY) {
            if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                if (gamePlay.racket.x <= 20) {
                    gamePlay.racket.x = 20;
                } else
                    gamePlay.racket.x -= 30;
            } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                if (gamePlay.racket.x + gamePlay.racket.w >= 780)
                    gamePlay.racket.x = 650;
                else
                    gamePlay.racket.x += 30;
            }
        }

        repaint();
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }

    @Override
    public void run() {
        int t_sleep = 33;
        Point p = new Point();


        while (true) {
            for (var e : gamePlay.balls)
                e.update((float) (0.030f * (1 + gamePlay.stage * 0.3)), gamePlay.racket);

            var it1 = gamePlay.balls.iterator();
            while (it1.hasNext()) {
                Ball b = it1.next();
                p = b.collisionResolution(gamePlay.blocks);
                if (b.y > 800 || b.y < 0 || b.x < 0 || b.x > 800) {
                    new Thread(() -> {
                        SoundManager.playSound("BallFall.wav");
                    }).start();
                    it1.remove();
                }
            }

            var it = gamePlay.blocks.iterator();
            while (it.hasNext()) {
                Block b = it.next();
                if (b.isCollision) {
                    if (b.isYellow) {
                        new Thread(() -> {
                            SoundManager.playSound("YellowBlockDestroyed.wav");
                        }).start();
                    } else {
                        new Thread(() -> {
                            SoundManager.playSound("BlockDestroyed.wav");
                        }).start();
                    }
                    if (b.isYellow && p.x != 0 && p.y != 0) {
                        Ball ball = new Ball(gamePlay, p.x, p.y, 150, 150);
                        Ball ball2 = new Ball(gamePlay, p.x, p.y, 150, 150);
                        gamePlay.addBall(ball);
                        gamePlay.addBall(ball2);
                    }
                    it.remove();
                }
            }

            if (mode == GAME_START) { // 스페이스바 문구 반짝이게
                t_sleep = 100;
                if (flag == 1) {
                    title.str4 = "";
                    flag = 0;
                } else if (flag == 0) {
                    title.str4 = "Press Spacebar to play!";
                    flag = 1;
                }
            } else if (mode == GAME_OVER) {
                t_sleep = 100;
                if (flag == 1) {
                    gameOver.str4 = "";
                    flag = 0;
                } else if (flag == 0) {
                    gameOver.str4 = "Press Spacebar!";
                    flag = 1;
                }
            } else if (mode == GAME_PLAY) {
                t_sleep = 15 - gamePlay.stage; // sleep 시간 조절을 통해 스테이지 별 공의 속도 조절
                if (gamePlay.blocks.isEmpty()) {
                    new Thread(() -> {
                        SoundManager.playSound("StageClear.wav");
                    }).start();
                    try {
                        Thread.sleep(1000); // 1초 쉬기
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    gamePlay.changeStage();
                }

                if (gamePlay.balls.isEmpty()) {
                    mode = GAME_OVER;
                }
            }

            try {
                Thread.sleep(t_sleep);
            } catch (InterruptedException e) {
                return;
            }

            repaint();
        }

    }
}

class SoundManager {
    private static Clip currentClip;
    private static final Map<String, Clip> clipCache = new HashMap<>();
    private static Clip bgmClip;  // 배경음악용 클립

    public static void playBGM(String filename) {
        try {
            if (bgmClip != null) {
                bgmClip.stop();
                bgmClip.close();
            }

            AudioInputStream audioIn = AudioSystem.getAudioInputStream(
                    SoundManager.class.getResource(filename));
            bgmClip = AudioSystem.getClip();
            bgmClip.open(audioIn);

            // 무한 반복 설정
            bgmClip.loop(Clip.LOOP_CONTINUOUSLY);
            bgmClip.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void stopBGM() {
        if (bgmClip != null && bgmClip.isRunning()) {
            bgmClip.stop();
            bgmClip.close();
        }
    }

    public static void playSound(String filename) {
        try {
            // 캐시된 클립이 있으면 재사용
            Clip clip = clipCache.get(filename);
            if (clip == null) {
                AudioInputStream audioIn = AudioSystem.getAudioInputStream(
                        SoundManager.class.getResource(filename));
                clip = AudioSystem.getClip();
                clip.open(audioIn);
                clipCache.put(filename, clip);
            }

            clip.setFramePosition(0);  // 클립을 처음으로 되감기
            clip.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void stopCurrentSound() {
        if (currentClip != null && currentClip.isRunning()) {
            currentClip.stop();
        }
    }
}

public class JavaHW5 extends JFrame {
    JavaHW5() {
        setTitle("Java Homework5");
        setSize(800, 800);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        add(new JavaHW5Panel());

        setVisible(true);
    }

    public static void main(String[] args) {

        new JavaHW5();
    }
}
