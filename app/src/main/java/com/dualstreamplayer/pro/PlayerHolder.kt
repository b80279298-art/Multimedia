package com.dualstreamplayer.pro

import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

/**
 * PlayerHolder encapsula um ExoPlayer, seu PlayerView associado,
 * a URL da stream, o volume atual e o estado de atividade.
 *
 * Cada instância representa um canal de vídeo independente.
 */
data class PlayerHolder(
    /** Índice do player (0–3) para identificação */
    val index: Int,

    /** Instância do ExoPlayer responsável pela decodificação e reprodução */
    val player: ExoPlayer,

    /** View que renderiza o vídeo na tela */
    val playerView: PlayerView,

    /** URL da stream HLS (.m3u8) atualmente carregada */
    var streamUrl: String = "",

    /**
     * Volume do player no intervalo [0.0, 1.0].
     * Este valor é sincronizado com player.volume ao ser alterado.
     */
    var volume: Float = 1.0f,

    /**
     * Indica se este player está ativo na disposição atual.
     * Players inativos têm sua view ocultada para economizar recursos.
     */
    var isVisible: Boolean = true,

    /**
     * Nível de foco de áudio deste player.
     * AudioFocusLevel.PRIMARY   → volume máximo (1.0 × volume do usuário)
     * AudioFocusLevel.SECONDARY → volume reduzido (0.3 × volume do usuário)
     * AudioFocusLevel.NONE      → sem foco especial
     */
    var audioFocusLevel: AudioFocusLevel = AudioFocusLevel.NONE
)

/**
 * Níveis de foco de áudio suportados pelo sistema de múltiplos áudios.
 */
enum class AudioFocusLevel {
    /** Stream principal — recebe volume total */
    PRIMARY,

    /** Stream secundária — recebe volume reduzido (30 %) */
    SECONDARY,

    /** Sem foco de áudio especial */
    NONE
}
