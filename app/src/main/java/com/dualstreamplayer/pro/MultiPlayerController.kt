package com.dualstreamplayer.pro

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.ui.PlayerView

/**
 * MultiPlayerController gerencia os quatro ExoPlayers independentes.
 *
 * Responsabilidades:
 *  - Criar e liberar os players
 *  - Carregar URLs HLS (.m3u8) em cada player
 *  - Controlar volume individual e sistema de foco de áudio
 *  - Configurar buffer, timeout de rede e reconexão automática
 *  - Expor PlayerViews para o LayoutManager
 */
class MultiPlayerController(private val context: Context) {

    companion object {
        private const val TAG = "MultiPlayerController"
        const val PLAYER_COUNT = 4

        /** Volume aplicado à stream com foco PRIMARY */
        const val FOCUS_PRIMARY_MULTIPLIER = 1.0f

        /** Volume aplicado à stream com foco SECONDARY (30 % do volume do usuário) */
        const val FOCUS_SECONDARY_MULTIPLIER = 0.30f
    }

    // ──────────────────────────────────────────────────────────
    // Estado
    // ──────────────────────────────────────────────────────────

    /** Lista dos quatro PlayerHolders (índices 0–3) */
    val holders = mutableListOf<PlayerHolder>()

    /** Configurações atuais de buffer e rede */
    var settings = PlayerSettings()

    /** Modo de múltiplos áudios: quando false apenas o PRIMARY toca com volume cheio */
    var multiAudioEnabled: Boolean = true

    // ──────────────────────────────────────────────────────────
    // Inicialização
    // ──────────────────────────────────────────────────────────

    /**
     * Inicializa os quatro players e os associa às views fornecidas.
     * Deve ser chamado uma única vez na criação da Activity.
     *
     * @param playerViews Lista de exatamente 4 PlayerViews na ordem 1→4
     */
    fun initialize(playerViews: List<PlayerView>) {
        require(playerViews.size == PLAYER_COUNT) {
            "São necessários exatamente $PLAYER_COUNT PlayerViews"
        }

        for (i in 0 until PLAYER_COUNT) {
            val player = buildExoPlayer()
            val holder = PlayerHolder(
                index = i,
                player = player,
                playerView = playerViews[i],
                volume = 1.0f,
                audioFocusLevel = if (i == 0) AudioFocusLevel.PRIMARY else AudioFocusLevel.NONE
            )
            playerViews[i].player = player
            // Ocultar os controles padrão do ExoPlayer — usamos nossa própria UI
            playerViews[i].useController = false
            holders.add(holder)
        }

        Log.d(TAG, "Quatro players inicializados com sucesso")
    }

    /**
     * Constrói um ExoPlayer configurado com os parâmetros atuais de [settings].
     */
    private fun buildExoPlayer(): ExoPlayer {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        return ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus= */ false)
            .build()
            .also { player ->
                player.repeatMode = Player.REPEAT_MODE_ALL
                player.playWhenReady = true
                player.addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        Log.e(TAG, "Erro no player: ${error.message}")
                        // Reconexão automática se habilitada
                        if (settings.autoReconnect) {
                            player.prepare()
                        }
                    }
                })
            }
    }

    // ──────────────────────────────────────────────────────────
    // Controle de streams
    // ──────────────────────────────────────────────────────────

    /**
     * Define a URL da stream para o player no [index].
     * A stream é carregada imediatamente sem reiniciar outros players.
     *
     * @param index  Índice do player (0–3)
     * @param url    URL HLS (.m3u8). Se vazio, o player é parado.
     */
    fun setStreamUrl(index: Int, url: String) {
        require(index in 0 until PLAYER_COUNT)
        val holder = holders[index]
        holder.streamUrl = url

        if (url.isBlank()) {
            holder.player.stop()
            Log.d(TAG, "Player $index: stream removida")
            return
        }

        val mediaSource = buildHlsMediaSource(url)
        holder.player.setMediaSource(mediaSource)
        holder.player.prepare()
        holder.player.playWhenReady = true
        applyVolume(holder)
        Log.d(TAG, "Player $index: carregando URL = $url")
    }

    /**
     * Cria um HlsMediaSource com o DataSource configurado pelos [settings] atuais.
     */
    private fun buildHlsMediaSource(url: String): MediaSource {
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(settings.networkTimeoutMs)
            .setReadTimeoutMs(settings.networkTimeoutMs)
            .setAllowCrossProtocolRedirects(true)

        return HlsMediaSource.Factory(dataSourceFactory)
            .setAllowChunklessPreparation(true)
            .createMediaSource(MediaItem.fromUri(Uri.parse(url)))
    }

    // ──────────────────────────────────────────────────────────
    // Controle de volume
    // ──────────────────────────────────────────────────────────

    /**
     * Define o volume do usuário para o player [index] no intervalo [0.0, 1.0].
     * O volume efetivo considera o foco de áudio do player.
     */
    fun setUserVolume(index: Int, volume: Float) {
        require(index in 0 until PLAYER_COUNT)
        holders[index].volume = volume.coerceIn(0f, 1f)
        applyVolume(holders[index])
    }

    /**
     * Define qual player é a stream PRINCIPAL (AudioFocusLevel.PRIMARY).
     * O player anterior com PRIMARY é rebaixado para NONE (ou SECONDARY se
     * [secondaryIndex] for fornecido).
     *
     * @param primaryIndex   Índice do novo player principal
     * @param secondaryIndex Índice do player secundário (-1 para nenhum)
     */
    fun setPrimaryPlayer(primaryIndex: Int, secondaryIndex: Int = -1) {
        for (holder in holders) {
            holder.audioFocusLevel = when (holder.index) {
                primaryIndex -> AudioFocusLevel.PRIMARY
                secondaryIndex -> AudioFocusLevel.SECONDARY
                else -> AudioFocusLevel.NONE
            }
            applyVolume(holder)
        }
        Log.d(TAG, "Foco de áudio: PRIMARY=$primaryIndex, SECONDARY=$secondaryIndex")
    }

    /**
     * Aplica o volume efetivo ao ExoPlayer considerando foco e modo de múltiplos áudios.
     */
    private fun applyVolume(holder: PlayerHolder) {
        val effectiveVolume = when {
            !multiAudioEnabled && holder.audioFocusLevel != AudioFocusLevel.PRIMARY -> 0f
            holder.audioFocusLevel == AudioFocusLevel.PRIMARY ->
                holder.volume * FOCUS_PRIMARY_MULTIPLIER
            holder.audioFocusLevel == AudioFocusLevel.SECONDARY ->
                holder.volume * FOCUS_SECONDARY_MULTIPLIER
            else -> holder.volume
        }
        holder.player.volume = effectiveVolume
    }

    /**
     * Reaplicar os volumes de todos os holders (útil ao alterar [multiAudioEnabled]).
     */
    fun refreshAllVolumes() {
        holders.forEach { applyVolume(it) }
    }

    // ──────────────────────────────────────────────────────────
    // Controle de reprodução global
    // ──────────────────────────────────────────────────────────

    /** Pausa todos os players */
    fun pauseAll() = holders.forEach { it.player.pause() }

    /** Resume todos os players */
    fun resumeAll() = holders.forEach { it.player.play() }

    /** Retorna true se todos os players visíveis estiverem reproduzindo */
    fun isPlayingAny(): Boolean = holders.any { it.isVisible && it.player.isPlaying }

    // ──────────────────────────────────────────────────────────
    // Aplicar configurações
    // ──────────────────────────────────────────────────────────

    /**
     * Aplica novas [PlayerSettings] recarregando as streams afetadas.
     * Somente as propriedades que exigem rebuild do player são reaplicadas.
     */
    fun applySettings(newSettings: PlayerSettings) {
        val urlsToReload = mutableListOf<Pair<Int, String>>()

        // Se o timeout mudou, precisamos recarregar as streams
        if (newSettings.networkTimeoutMs != settings.networkTimeoutMs) {
            holders.forEach { holder ->
                if (holder.streamUrl.isNotBlank()) {
                    urlsToReload.add(Pair(holder.index, holder.streamUrl))
                }
            }
        }

        settings = newSettings
        multiAudioEnabled = newSettings.multiAudioEnabled

        // Recarregar streams afetadas
        urlsToReload.forEach { (index, url) ->
            setStreamUrl(index, url)
        }

        // Reaplicar volumes
        refreshAllVolumes()
        Log.d(TAG, "Configurações aplicadas: $newSettings")
    }

    // ──────────────────────────────────────────────────────────
    // Ciclo de vida
    // ──────────────────────────────────────────────────────────

    /** Libera todos os ExoPlayers — chamar no onDestroy da Activity */
    fun releaseAll() {
        holders.forEach { holder ->
            holder.player.stop()
            holder.player.release()
        }
        holders.clear()
        Log.d(TAG, "Todos os players liberados")
    }
}

// ──────────────────────────────────────────────────────────
// Modelo de configurações
// ──────────────────────────────────────────────────────────

/**
 * Configurações do player persistidas pelo usuário.
 *
 * @param bufferMs           Duração mínima de buffer em ms (padrão: 5 000)
 * @param networkTimeoutMs   Timeout de conexão e leitura em ms (padrão: 15 000)
 * @param autoReconnect      Reconectar automaticamente em caso de erro
 * @param multiAudioEnabled  Permitir múltiplos áudios simultâneos
 * @param primaryPlayerIndex Índice do player que será o principal (0–3)
 */
data class PlayerSettings(
    val bufferMs: Int = 5_000,
    val networkTimeoutMs: Int = 15_000,
    val autoReconnect: Boolean = true,
    val multiAudioEnabled: Boolean = true,
    val primaryPlayerIndex: Int = 0
)
