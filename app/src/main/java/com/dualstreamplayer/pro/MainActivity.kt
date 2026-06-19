package com.dualstreamplayer.pro

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.View
import android.view.WindowInsetsController
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.ui.PlayerView
import com.dualstreamplayer.pro.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider

/**
 * MainActivity — ponto de entrada do DualStreamPlayer Pro.
 *
 * Coordena:
 *  - Inicialização dos quatro ExoPlayers via [MultiPlayerController]
 *  - Troca de layouts via [LayoutManager]
 *  - Barra superior de ações (Layout, Fullscreen, Trocar Áudio, Configurações)
 *  - Barra inferior com os 8 ícones de layout
 *  - Diálogo de configurações
 *  - Suporte a Tela Cheia e Picture-in-Picture (Android 8+)
 */
class MainActivity : AppCompatActivity() {

    // ──────────────────────────────────────────────────────────
    // Campos
    // ──────────────────────────────────────────────────────────

    private lateinit var binding: ActivityMainBinding

    /** Controla os quatro ExoPlayers */
    private val playerController = MultiPlayerController(this@MainActivity)

    /** Gerencia os 8 modos de layout */
    private lateinit var layoutManager: LayoutManager

    /** Lista ordenada dos PlayerViews (mesma ordem dos holders) */
    private val playerViews: MutableList<PlayerView> = mutableListOf()

    /** Modo de tela cheia ativo */
    private var isFullscreen = false

    /** Índice do player primário atual (para alternância de áudio) */
    private var primaryPlayerIndex = 0

    /** Índice do player secundário atual */
    private var secondaryPlayerIndex = 1

    // ──────────────────────────────────────────────────────────
    // Ciclo de vida
    // ──────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configurar edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupPlayerViews()
        setupLayoutManager()
        setupTopBar()
        setupLayoutBar()
        loadDefaultStreams()
    }

    override fun onResume() {
        super.onResume()
        playerController.resumeAll()
    }

    override fun onPause() {
        super.onPause()
        // Não pausar se estiver em PiP
        if (!isInPictureInPictureMode) {
            playerController.pauseAll()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        playerController.releaseAll()
    }

    // ──────────────────────────────────────────────────────────
    // Configuração dos PlayerViews
    // ──────────────────────────────────────────────────────────

    /**
     * Coleta os quatro PlayerViews do layout e inicializa o [MultiPlayerController].
     */
    private fun setupPlayerViews() {
        playerViews.apply {
            add(binding.playerView1)
            add(binding.playerView2)
            add(binding.playerView3)
            add(binding.playerView4)
        }
        playerController.initialize(playerViews)
    }

    // ──────────────────────────────────────────────────────────
    // Configuração do LayoutManager
    // ──────────────────────────────────────────────────────────

    private fun setupLayoutManager() {
        layoutManager = LayoutManager(binding.videoContainer, playerViews)
        // Aplicar layout inicial (tela única)
        layoutManager.applyLayout(LayoutMode.SINGLE)
    }

    // ──────────────────────────────────────────────────────────
    // Barra superior de ações
    // ──────────────────────────────────────────────────────────

    private fun setupTopBar() {
        // Botão Layout — mostra popup com os 8 modos
        binding.btnLayout.setOnClickListener { showLayoutSelectionDialog() }

        // Botão Tela Cheia
        binding.btnFullscreen.setOnClickListener { toggleFullscreen() }

        // Botão Trocar Áudio — alterna qual player é o principal
        binding.btnSwitchAudio.setOnClickListener { switchAudioFocus() }

        // Botão Configurações
        binding.btnSettings.setOnClickListener { showSettingsDialog() }
    }

    // ──────────────────────────────────────────────────────────
    // Barra inferior de layouts (8 ícones)
    // ──────────────────────────────────────────────────────────

    private fun setupLayoutBar() {
        val layoutButtons = listOf(
            binding.btnLayoutSingle,
            binding.btnLayoutHorizontal,
            binding.btnLayoutVertical,
            binding.btnLayoutPip,
            binding.btnLayoutMainPlus2,
            binding.btnLayoutMainPlus3,
            binding.btnLayoutGrid,
            binding.btnLayoutT
        )
        val modes = LayoutMode.values()

        layoutButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                val mode = modes[index]
                layoutManager.applyLayout(mode)
                // Atualizar estado visual dos botões
                layoutButtons.forEach { btn ->
                    btn.isSelected = (btn == button)
                }
            }
        }

        // Selecionar o primeiro por padrão
        layoutButtons.firstOrNull()?.isSelected = true
    }

    // ──────────────────────────────────────────────────────────
    // Diálogo de seleção de layout
    // ──────────────────────────────────────────────────────────

    private fun showLayoutSelectionDialog() {
        val items = LayoutMode.values().map { getString(it.labelRes) }.toTypedArray()
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_select_layout)
            .setItems(items) { _, which ->
                layoutManager.applyLayout(LayoutMode.values()[which])
            }
            .show()
    }

    // ──────────────────────────────────────────────────────────
    // Tela Cheia
    // ──────────────────────────────────────────────────────────

    /**
     * Alterna entre o modo de tela cheia e o modo normal.
     * Oculta barras de status e navegação quando ativo.
     */
    private fun toggleFullscreen() {
        isFullscreen = !isFullscreen
        val insetsController = WindowInsetsControllerCompat(window, binding.root)

        if (isFullscreen) {
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            binding.btnFullscreen.setImageResource(R.drawable.ic_fullscreen_exit)
            // Ocultar barras da UI para tela cheia total
            binding.topBar.visibility    = View.GONE
            binding.layoutBar.visibility = View.GONE
        } else {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
            binding.btnFullscreen.setImageResource(R.drawable.ic_fullscreen)
            binding.topBar.visibility    = View.VISIBLE
            binding.layoutBar.visibility = View.VISIBLE
        }
    }

    // ──────────────────────────────────────────────────────────
    // Sistema de Foco de Áudio — alternância rápida
    // ──────────────────────────────────────────────────────────

    /**
     * Alterna ciclicamente qual player é o PRIMARY.
     * Ciclo: 0 → 1 → 2 → 3 → 0 → ...
     * O player anterior passa a ser SECONDARY.
     */
    private fun switchAudioFocus() {
        secondaryPlayerIndex = primaryPlayerIndex
        primaryPlayerIndex = (primaryPlayerIndex + 1) % MultiPlayerController.PLAYER_COUNT
        playerController.setPrimaryPlayer(primaryPlayerIndex, secondaryPlayerIndex)

        val playerNumber = primaryPlayerIndex + 1
        Toast.makeText(
            this,
            getString(R.string.audio_focus_switched, playerNumber),
            Toast.LENGTH_SHORT
        ).show()
        updateAudioFocusButton()
    }

    /** Atualiza o ícone do botão de áudio para refletir qual player é o primário */
    private fun updateAudioFocusButton() {
        binding.btnSwitchAudio.text = getString(
            R.string.audio_label,
            primaryPlayerIndex + 1
        )
    }

    // ──────────────────────────────────────────────────────────
    // Diálogo de Configurações
    // ──────────────────────────────────────────────────────────

    /**
     * Exibe o diálogo completo de configurações com todas as opções disponíveis.
     */
    private fun showSettingsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_settings, null)

        // ── Referências às views do diálogo ──
        val etStream1     = dialogView.findViewById<EditText>(R.id.etStream1)
        val etStream2     = dialogView.findViewById<EditText>(R.id.etStream2)
        val etStream3     = dialogView.findViewById<EditText>(R.id.etStream3)
        val etStream4     = dialogView.findViewById<EditText>(R.id.etStream4)
        val sliderVol1    = dialogView.findViewById<Slider>(R.id.sliderVolume1)
        val sliderVol2    = dialogView.findViewById<Slider>(R.id.sliderVolume2)
        val sliderVol3    = dialogView.findViewById<Slider>(R.id.sliderVolume3)
        val sliderVol4    = dialogView.findViewById<Slider>(R.id.sliderVolume4)
        val switchMulti   = dialogView.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switchMultiAudio)
        val switchRecon   = dialogView.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switchAutoReconnect)
        val etBuffer      = dialogView.findViewById<EditText>(R.id.etBuffer)
        val etTimeout     = dialogView.findViewById<EditText>(R.id.etTimeout)
        val spinnerPrimary = dialogView.findViewById<Spinner>(R.id.spinnerPrimary)

        // ── Preencher com valores atuais ──
        etStream1.setText(playerController.holders.getOrNull(0)?.streamUrl ?: "")
        etStream2.setText(playerController.holders.getOrNull(1)?.streamUrl ?: "")
        etStream3.setText(playerController.holders.getOrNull(2)?.streamUrl ?: "")
        etStream4.setText(playerController.holders.getOrNull(3)?.streamUrl ?: "")

        sliderVol1.value = (playerController.holders.getOrNull(0)?.volume ?: 1f) * 100f
        sliderVol2.value = (playerController.holders.getOrNull(1)?.volume ?: 1f) * 100f
        sliderVol3.value = (playerController.holders.getOrNull(2)?.volume ?: 1f) * 100f
        sliderVol4.value = (playerController.holders.getOrNull(3)?.volume ?: 1f) * 100f

        switchMulti.isChecked  = playerController.settings.multiAudioEnabled
        switchRecon.isChecked  = playerController.settings.autoReconnect
        etBuffer.setText(playerController.settings.bufferMs.toString())
        etTimeout.setText(playerController.settings.networkTimeoutMs.toString())

        // Spinner do player principal
        val playerNames = arrayOf("Player 1", "Player 2", "Player 3", "Player 4")
        spinnerPrimary.adapter = ArrayAdapter(this,
            android.R.layout.simple_spinner_item, playerNames).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinnerPrimary.setSelection(playerController.settings.primaryPlayerIndex)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.settings_title)
            .setView(dialogView)
            .setPositiveButton(R.string.settings_apply) { _, _ ->
                // Aplicar URLs sem reiniciar players
                val urls = listOf(
                    etStream1.text.toString().trim(),
                    etStream2.text.toString().trim(),
                    etStream3.text.toString().trim(),
                    etStream4.text.toString().trim()
                )
                urls.forEachIndexed { i, url ->
                    val currentUrl = playerController.holders.getOrNull(i)?.streamUrl ?: ""
                    if (url != currentUrl) {
                        playerController.setStreamUrl(i, url)
                    }
                }

                // Aplicar volumes
                playerController.setUserVolume(0, sliderVol1.value / 100f)
                playerController.setUserVolume(1, sliderVol2.value / 100f)
                playerController.setUserVolume(2, sliderVol3.value / 100f)
                playerController.setUserVolume(3, sliderVol4.value / 100f)

                // Aplicar configurações
                val newSettings = PlayerSettings(
                    bufferMs           = etBuffer.text.toString().toIntOrNull()
                                            ?: playerController.settings.bufferMs,
                    networkTimeoutMs   = etTimeout.text.toString().toIntOrNull()
                                            ?: playerController.settings.networkTimeoutMs,
                    autoReconnect      = switchRecon.isChecked,
                    multiAudioEnabled  = switchMulti.isChecked,
                    primaryPlayerIndex = spinnerPrimary.selectedItemPosition
                )
                playerController.applySettings(newSettings)

                // Atualizar foco de áudio
                primaryPlayerIndex = newSettings.primaryPlayerIndex
                playerController.setPrimaryPlayer(primaryPlayerIndex)
                updateAudioFocusButton()

                Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    // ──────────────────────────────────────────────────────────
    // Streams padrão
    // ──────────────────────────────────────────────────────────

    /**
     * Carrega streams de demonstração públicas para facilitar o primeiro teste.
     * O usuário pode alterar as URLs pelo diálogo de Configurações.
     */
    private fun loadDefaultStreams() {
        val defaultUrls = listOf(
            "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
            "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_4x3/bipbop_4x3_variant.m3u8",
            "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
            "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_fmp4/master.m3u8"
        )
        defaultUrls.forEachIndexed { index, url ->
            playerController.setStreamUrl(index, url)
        }

        // Player 1 como primário, player 2 como secundário
        playerController.setPrimaryPlayer(primaryPlayerIndex, secondaryPlayerIndex)
        updateAudioFocusButton()
    }

    // ──────────────────────────────────────────────────────────
    // Picture-in-Picture
    // ──────────────────────────────────────────────────────────

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        enterPipIfSupported()
    }

    /**
     * Entra em modo PiP (Android 8+) com proporção 16:9 usando o player principal.
     */
    private fun enterPipIfSupported() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            enterPictureInPictureMode(params)
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPipMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPipMode, newConfig)
        if (isInPipMode) {
            // PiP: mostrar apenas player principal, ocultar UI
            binding.topBar.visibility    = View.GONE
            binding.layoutBar.visibility = View.GONE
            layoutManager.applyLayout(LayoutMode.SINGLE)
        } else {
            // Voltar da PiP: restaurar UI
            if (!isFullscreen) {
                binding.topBar.visibility    = View.VISIBLE
                binding.layoutBar.visibility = View.VISIBLE
            }
        }
    }

    // ──────────────────────────────────────────────────────────
    // Configuração de mudança de configuração (rotação, etc.)
    // ──────────────────────────────────────────────────────────

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // O ConstraintLayout se adapta automaticamente.
        // Reaplicar o layout atual para garantir proporções corretas.
        layoutManager.applyLayout(layoutManager.currentMode)
    }
}
