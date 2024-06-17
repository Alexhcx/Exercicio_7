package com.example.exercicio_7;

import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private static final long TEMPO_MAXIMO_MILISEGUNDOS = 120000; // 2 minutos em milissegundos
    private TextView textViewTempo;
    private Button botaoIniciar;
    private CountDownTimer timer;
    private Handler handler = new Handler();
    private boolean timerEmExecucao = false;
    private ImageView personagem;
    private int personagemX, personagemY;
    private int tamanhoPersonagem;
    private ImageView backgroundView;
    private ImageView moeda;
    private int moedaX, moedaY;
    private int tamanhoMoeda;
    private boolean movimentoLiberado = false;
    private RelativeLayout gameArea;
    private int pontos = 0;
    private TextView textViewPontos;
    private Random random = new Random();
    private TextView highScoreView;
    private Handler moedaHandler = new Handler(); // Handler para gerenciar a moeda
    private SharedPreferences sharedPreferences;
    private static final String HIGH_SCORE_KEY = "highScore";
    private MediaPlayer musicaFundo;
    private MediaPlayer somMovimento;
    private MediaPlayer somColisao;
    private MediaPlayer somTempoEsgotado;
    private boolean somMovimentoEmExecucao = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textViewTempo = findViewById(R.id.timerView);
        botaoIniciar = findViewById(R.id.bStart);
        personagem = findViewById(R.id.personagemView);
        backgroundView = findViewById(R.id.backgroundView);
        moeda = findViewById(R.id.moedaView);
        gameArea = findViewById(R.id.gameArea);
        textViewPontos = findViewById(R.id.scoreView);
        highScoreView = findViewById(R.id.HighscoreView);

        sharedPreferences = getSharedPreferences("gameData", MODE_PRIVATE);
        carregarHighScore(); // Carrega o high score ao iniciar a Activity

        // Obter o tamanho do personagem e da moeda
        tamanhoPersonagem = personagem.getWidth();
        tamanhoMoeda = moeda.getWidth();

        // Posicionar o personagem inicialmente no centro da tela
        personagemX = (backgroundView.getWidth() - tamanhoPersonagem) / 2;
        personagemY = (backgroundView.getHeight() - tamanhoPersonagem) / 2;
        personagem.setX(personagemX);
        personagem.setY(personagemY);

        // Esconder a moeda inicialmente
        moeda.setVisibility(View.INVISIBLE);

        // Inicializa os sons e a música
        musicaFundo = MediaPlayer.create(this, R.raw.backgroundmusic1);
        somMovimento = MediaPlayer.create(this, R.raw.passos1);
        somColisao = MediaPlayer.create(this, R.raw.pegarmoeda);
        somTempoEsgotado = MediaPlayer.create(this, R.raw.timesup);

        // Configura a música de fundo e movimento para repetir indefinidamente
        musicaFundo.setLooping(true);
        somMovimento.setLooping(true);

        // Inicia a música de fundo
        musicaFundo.start();


        botaoIniciar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (timerEmExecucao) {
                    cancelarContagem();
                    botaoIniciar.setText("Iniciar");
                    movimentoLiberado = false;
                    moeda.setVisibility(View.INVISIBLE); // Esconder a moeda ao parar o timer
                    moedaHandler.removeCallbacksAndMessages(null); // Parar a geração da moeda
                } else {
                    iniciarContagem();
                    botaoIniciar.setText("Parar");
                    movimentoLiberado = true;
                    gerarMoeda(); // Gerar a moeda ao iniciar o timer
                }
            }
        });

        gameArea.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (movimentoLiberado) {
                    int x = (int) event.getX();
                    int y = (int) event.getY();

                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            moverPersonagem(x, y);
                            verificarColisao();
                            if (!somMovimentoEmExecucao) {
                                somMovimento.start();
                                somMovimentoEmExecucao = true;
                            }
                            break;
                        case MotionEvent.ACTION_MOVE:
                            moverPersonagem(x, y);
                            verificarColisao();
                            break;
                        case MotionEvent.ACTION_UP:
                            moverPersonagem(x, y);
                            verificarColisao();
                            somMovimento.pause(); // Pausar o som, não interromper
                            somMovimentoEmExecucao = false;
                            break;
                    }
                }
                return true;
            }
        });
    }

    private void moverPersonagem(int x, int y) {
        int novaX = personagemX + (x - personagemX) / 10;
        int novaY = personagemY + (y - personagemY) / 10;

        novaX = Math.max(0, Math.min(novaX, backgroundView.getWidth() - tamanhoPersonagem));
        novaY = Math.max(0, Math.min(novaY, backgroundView.getHeight() - tamanhoPersonagem));

        personagemX = novaX;
        personagemY = novaY;
        personagem.setX(personagemX);
        personagem.setY(personagemY);
    }

    private void gerarMoeda() {
        moedaX = random.nextInt(backgroundView.getWidth() - tamanhoMoeda);
        moedaY = random.nextInt(backgroundView.getHeight() - tamanhoMoeda);
        moeda.setX(moedaX);
        moeda.setY(moedaY);
        moeda.setVisibility(View.VISIBLE);

        // Mostrar a moeda por 3 segundos
        moedaHandler.postDelayed(() -> {
            moeda.setVisibility(View.INVISIBLE);
            if (timerEmExecucao) { // Só gera nova moeda se o timer estiver rodando
                gerarMoeda(); // Gerar novamente após 3 segundos
            }
        }, 800);
    }

    private void verificarColisao() {
        if (moeda.getVisibility() == View.VISIBLE) {
            int centroPersonagemX = personagemX + tamanhoPersonagem / 2;
            int centroPersonagemY = personagemY + tamanhoPersonagem / 2;

            int centroMoedaX = moedaX + tamanhoMoeda / 2;
            int centroMoedaY = moedaY + tamanhoMoeda / 2;

            if (Math.abs(centroPersonagemX - centroMoedaX) < tamanhoPersonagem / 2 &&
                    Math.abs(centroPersonagemY - centroMoedaY) < tamanhoPersonagem / 2) {
                pontos++;
                textViewPontos.setText("Pontos: " + pontos);
                moeda.setVisibility(View.INVISIBLE); // Esconder a moeda após a colisão
                //gerarMoeda(); // Gerar uma nova moeda

                if (somColisao != null) {
                    somColisao.start();
                }

                // Verifica e atualiza o high score
                if (pontos > getHighScore()) {
                    salvarHighScore(pontos);
                    atualizarHighScoreView();
                }
            }
        }
    }

    private void salvarHighScore(int score) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(HIGH_SCORE_KEY, score);
        editor.apply();
    }

    private int getHighScore() {
        return sharedPreferences.getInt(HIGH_SCORE_KEY, 0); // 0 é o valor padrão
    }

    private void carregarHighScore() {
        atualizarHighScoreView();
    }

    private void atualizarHighScoreView() {
        highScoreView.setText("High Score: " + getHighScore());
    }

    private void iniciarContagem() {
        pontos = 0; // Reinicia os pontos quando o jogo inicia
        textViewPontos.setText("Pontos: " + pontos);
        timer = new CountDownTimer(TEMPO_MAXIMO_MILISEGUNDOS, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                handler.post(() -> {
                    textViewTempo.setText("Tempo restante: " + millisUntilFinished / 1000 + " seg.");
                });
            }

            @Override
            public void onFinish() {
                textViewTempo.setText("Tempo máximo expirado!");
                timerEmExecucao = false;
                botaoIniciar.setText("Iniciar");
                movimentoLiberado = false;
                moeda.setVisibility(View.INVISIBLE); // Esconder a moeda ao terminar o timer
                moedaHandler.removeCallbacksAndMessages(null); // Parar a geração da moeda
                if (somTempoEsgotado != null) {
                    somTempoEsgotado.start();
                }
            }
        }.start();
        timerEmExecucao = true;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            // Obter o tamanho do personagem e da moeda APÓS o layout ser desenhado
            tamanhoPersonagem = personagem.getWidth();
            tamanhoMoeda = moeda.getWidth();

            // Posicionar o personagem inicialmente no centro da tela
            personagemX = (backgroundView.getWidth() - tamanhoPersonagem) / 2;
            personagemY = (backgroundView.getHeight() - tamanhoPersonagem) / 2;
            personagem.setX(personagemX);
            personagem.setY(personagemY);
        }
    }

    private void cancelarContagem() {
        if (timer != null) {
            timer.cancel();
            timerEmExecucao = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelarContagem();
        moedaHandler.removeCallbacksAndMessages(null); // Parar a geração da moeda ao destruir a activity
        if (musicaFundo != null) {
            musicaFundo.release();
            musicaFundo = null;
        }
        if (somMovimento != null) {
            somMovimento.release();
            somMovimento = null;
        }
        if (somColisao != null) {
            somColisao.release();
            somColisao = null;
        }
        if (somTempoEsgotado != null) {
            somTempoEsgotado.release();
            somTempoEsgotado = null;
        }
    }
}