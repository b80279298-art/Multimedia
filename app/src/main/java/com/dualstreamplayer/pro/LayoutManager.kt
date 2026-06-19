package com.dualstreamplayer.pro

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.transition.ChangeBounds
import androidx.transition.Transition
import androidx.transition.TransitionManager
import androidx.media3.ui.PlayerView

/**
 * LayoutManager gerencia os 8 modos de visualização disponíveis,
 * reorganizando os quatro PlayerViews dentro do ConstraintLayout
 * com animações suaves de transição.
 *
 * Os modos disponíveis estão descritos em [LayoutMode].
 */
class LayoutManager(
    /** O ConstraintLayout raiz que contém os quatro PlayerViews */
    private val container: ConstraintLayout,
    /** Os quatro PlayerViews na ordem player1, player2, player3, player4 */
    private val playerViews: List<PlayerView>
) {
    companion object {
        private const val TAG = "LayoutManager"

        /** Duração da animação de transição entre layouts em ms */
        private const val TRANSITION_DURATION_MS = 300L
    }

    /** Modo de layout atualmente ativo */
    var currentMode: LayoutMode = LayoutMode.SINGLE
        private set

    // ──────────────────────────────────────────────────────────
    // API pública
    // ──────────────────────────────────────────────────────────

    /**
     * Aplica o [mode] de layout solicitado com animação suave.
     * Os ExoPlayers NÃO são reiniciados — apenas as views são reorganizadas.
     */
    fun applyLayout(mode: LayoutMode) {
        if (mode == currentMode) return
        Log.d(TAG, "Trocando layout: $currentMode → $mode")
        currentMode = mode
        animateToLayout(mode)
    }

    // ──────────────────────────────────────────────────────────
    // Animação e transição
    // ──────────────────────────────────────────────────────────

    /**
     * Executa a transição animada para o [mode] usando [ChangeBounds].
     * Isso garante que o redimensionamento e a movimentação sejam suaves
     * sem pausar a reprodução dos vídeos.
     */
    private fun animateToLayout(mode: LayoutMode) {
        val transition = ChangeBounds().apply {
            duration = TRANSITION_DURATION_MS
        }
        // TransitionManager.beginDelayedTransition captura o estado atual
        // e anima para o novo estado após a aplicação do ConstraintSet
        TransitionManager.beginDelayedTransition(container, transition)
        applyConstraints(mode)
    }

    // ──────────────────────────────────────────────────────────
    // Aplicação dos ConstraintSets
    // ──────────────────────────────────────────────────────────

    /**
     * Seleciona e aplica o ConstraintSet correspondente ao [mode].
     */
    private fun applyConstraints(mode: LayoutMode) {
        val cs = ConstraintSet()
        cs.clone(container)

        // Ocultar todos inicialmente — o método do modo reativa os necessários
        for (view in playerViews) {
            cs.setVisibility(view.id, ConstraintSet.GONE)
        }

        when (mode) {
            LayoutMode.SINGLE          -> constraintSingle(cs)
            LayoutMode.HORIZONTAL_SPLIT -> constraintHorizontalSplit(cs)
            LayoutMode.VERTICAL_SPLIT  -> constraintVerticalSplit(cs)
            LayoutMode.PIP             -> constraintPip(cs)
            LayoutMode.MAIN_PLUS_2     -> constraintMainPlus2(cs)
            LayoutMode.MAIN_PLUS_3     -> constraintMainPlus3(cs)
            LayoutMode.GRID_2X2        -> constraintGrid2x2(cs)
            LayoutMode.T_LAYOUT        -> constraintTLayout(cs)
        }

        cs.applyTo(container)
    }

    // ──────────────────────────────────────────────────────────
    // Modo 1 — Tela única (player1 ocupa tudo)
    // ──────────────────────────────────────────────────────────
    private fun constraintSingle(cs: ConstraintSet) {
        val v = playerViews[0]
        cs.setVisibility(v.id, ConstraintSet.VISIBLE)
        cs.connect(v.id, ConstraintSet.TOP,    ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        cs.connect(v.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        cs.connect(v.id, ConstraintSet.START,  ConstraintSet.PARENT_ID, ConstraintSet.START)
        cs.connect(v.id, ConstraintSet.END,    ConstraintSet.PARENT_ID, ConstraintSet.END)
        cs.constrainWidth(v.id,  0)
        cs.constrainHeight(v.id, 0)
    }

    // ──────────────────────────────────────────────────────────
    // Modo 2 — Tela dividida horizontal (player1 topo, player2 base)
    // ──────────────────────────────────────────────────────────
    private fun constraintHorizontalSplit(cs: ConstraintSet) {
        val top    = playerViews[0]
        val bottom = playerViews[1]

        cs.setVisibility(top.id,    ConstraintSet.VISIBLE)
        cs.setVisibility(bottom.id, ConstraintSet.VISIBLE)

        // Player 1 — metade superior
        cs.connect(top.id, ConstraintSet.TOP,    ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        cs.connect(top.id, ConstraintSet.BOTTOM, bottom.id,               ConstraintSet.TOP)
        cs.connect(top.id, ConstraintSet.START,  ConstraintSet.PARENT_ID, ConstraintSet.START)
        cs.connect(top.id, ConstraintSet.END,    ConstraintSet.PARENT_ID, ConstraintSet.END)
        cs.constrainWidth(top.id,  0)
        cs.constrainHeight(top.id, 0)

        // Player 2 — metade inferior
        cs.connect(bottom.id, ConstraintSet.TOP,    top.id,                  ConstraintSet.BOTTOM)
        cs.connect(bottom.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        cs.connect(bottom.id, ConstraintSet.START,  ConstraintSet.PARENT_ID, ConstraintSet.START)
        cs.connect(bottom.id, ConstraintSet.END,    ConstraintSet.PARENT_ID, ConstraintSet.END)
        cs.constrainWidth(bottom.id,  0)
        cs.constrainHeight(bottom.id, 0)

        // Garantir alturas iguais usando a barreira central
        cs.createHorizontalChain(
            ConstraintSet.PARENT_ID, ConstraintSet.TOP,
            ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM,
            intArrayOf(top.id, bottom.id), null,
            ConstraintSet.CHAIN_SPREAD_INSIDE
        )
    }

    // ──────────────────────────────────────────────────────────
    // Modo 3 — Tela dividida vertical (player1 esq, player2 dir)
    // ──────────────────────────────────────────────────────────
    private fun constraintVerticalSplit(cs: ConstraintSet) {
        val left  = playerViews[0]
        val right = playerViews[1]

        cs.setVisibility(left.id,  ConstraintSet.VISIBLE)
        cs.setVisibility(right.id, ConstraintSet.VISIBLE)

        // Player 1 — metade esquerda
        cs.connect(left.id, ConstraintSet.TOP,    ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        cs.connect(left.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        cs.connect(left.id, ConstraintSet.START,  ConstraintSet.PARENT_ID, ConstraintSet.START)
        cs.connect(left.id, ConstraintSet.END,    right.id,                ConstraintSet.START)
        cs.constrainWidth(left.id,  0)
        cs.constrainHeight(left.id, 0)

        // Player 2 — metade direita
        cs.connect(right.id, ConstraintSet.TOP,    ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        cs.connect(right.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        cs.connect(right.id, ConstraintSet.START,  left.id,                 ConstraintSet.END)
        cs.connect(right.id, ConstraintSet.END,    ConstraintSet.PARENT_ID, ConstraintSet.END)
        cs.constrainWidth(right.id,  0)
        cs.constrainHeight(right.id, 0)

        cs.createVerticalChain(
            ConstraintSet.PARENT_ID, ConstraintSet.START,
            ConstraintSet.PARENT_ID, ConstraintSet.END,
            intArrayOf(left.id, right.id), null,
            ConstraintSet.CHAIN_SPREAD_INSIDE
        )
    }

    // ──────────────────────────────────────────────────────────
    // Modo 4 — Picture in Picture (player1 full + player2 mini)
    // ──────────────────────────────────────────────────────────
    private fun constraintPip(cs: ConstraintSet) {
        val main = playerViews[0]
        val mini = playerViews[1]

        cs.setVisibility(main.id, ConstraintSet.VISIBLE)
        cs.setVisibility(mini.id, ConstraintSet.VISIBLE)

        // Player 1 — tela cheia
        cs.connect(main.id, ConstraintSet.TOP,    ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        cs.connect(main.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        cs.connect(main.id, ConstraintSet.START,  ConstraintSet.PARENT_ID, ConstraintSet.START)
        cs.connect(main.id, ConstraintSet.END,    ConstraintSet.PARENT_ID, ConstraintSet.END)
        cs.constrainWidth(main.id,  0)
        cs.constrainHeight(main.id, 0)

        // Player 2 — mini no canto inferior direito (25 % de largura)
        cs.connect(mini.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 16)
        cs.connect(mini.id, ConstraintSet.END,    ConstraintSet.PARENT_ID, ConstraintSet.END,    16)
        cs.constrainPercentWidth(mini.id,  0.25f)
        cs.constrainPercentHeight(mini.id, 0.25f)
        cs.setElevation(mini.id, 8f) // mini fica acima do main
    }

    // ──────────────────────────────────────────────────────────
    // Modo 5 — Principal + 2 miniaturas (player1 esq 2/3, player2/3 dir)
    // ──────────────────────────────────────────────────────────
    private fun constraintMainPlus2(cs: ConstraintSet) {
        val main   = playerViews[0]
        val thumb1 = playerViews[1]
        val thumb2 = playerViews[2]

        cs.setVisibility(main.id,   ConstraintSet.VISIBLE)
        cs.setVisibility(thumb1.id, ConstraintSet.VISIBLE)
        cs.setVisibility(thumb2.id, ConstraintSet.VISIBLE)

        // Player 1 — 2/3 da largura, altura total
        cs.connect(main.id, ConstraintSet.TOP,    ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        cs.connect(main.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        cs.connect(main.id, ConstraintSet.START,  ConstraintSet.PARENT_ID, ConstraintSet.START)
        cs.connect(main.id, ConstraintSet.END,    thumb1.id,               ConstraintSet.START)
        cs.constrainWidth(main.id,  0)
        cs.constrainHeight(main.id, 0)
        cs.constrainPercentWidth(main.id, 0.67f)

        // Miniatura superior direita
        cs.connect(thumb1.id, ConstraintSet.TOP,    ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        cs.connect(thumb1.id, ConstraintSet.BOTTOM, thumb2.id,               ConstraintSet.TOP)
        cs.connect(thumb1.id, ConstraintSet.START,  main.id,                 ConstraintSet.END)
        cs.connect(thumb1.id, ConstraintSet.END,    ConstraintSet.PARENT_ID, ConstraintSet.END)
        cs.constrainWidth(thumb1.id,  0)
        cs.constrainHeight(thumb1.id, 0)

        // Miniatura inferior direita
        cs.connect(thumb2.id, ConstraintSet.TOP,    thumb1.id,               ConstraintSet.BOTTOM)
        cs.connect(thumb2.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        cs.connect(thumb2.id, ConstraintSet.START,  main.id,                 ConstraintSet.END)
        cs.connect(thumb2.id, ConstraintSet.END,    ConstraintSet.PARENT_ID, ConstraintSet.END)
        cs.constrainWidth(thumb2.id,  0)
        cs.constrainHeight(thumb2.id, 0)
    }

    // ──────────────────────────────────────────────────────────
    // Modo 6 — Principal + 3 miniaturas (player1 esq, players 2/3/4 coluna dir)
    // ──────────────────────────────────────────────────────────
    private fun constraintMainPlus3(cs: ConstraintSet) {
        val main   = playerViews[0]
        val th1    = playerViews[1]
        val th2    = playerViews[2]
        val th3    = playerViews[3]

        cs.setVisibility(main.id, ConstraintSet.VISIBLE)
        cs.setVisibility(th1.id,  ConstraintSet.VISIBLE)
        cs.setVisibility(th2.id,  ConstraintSet.VISIBLE)
        cs.setVisibility(th3.id,  ConstraintSet.VISIBLE)

        // Player 1 — 65 % da largura
        cs.connect(main.id, ConstraintSet.TOP,    ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        cs.connect(main.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        cs.connect(main.id, ConstraintSet.START,  ConstraintSet.PARENT_ID, ConstraintSet.START)
        cs.connect(main.id, ConstraintSet.END,    th1.id,                  ConstraintSet.START)
        cs.constrainWidth(main.id,  0)
        cs.constrainHeight(main.id, 0)
        cs.constrainPercentWidth(main.id, 0.65f)

        // Coluna direita: th1, th2, th3 em cadeia vertical
        for ((prev, curr) in listOf(Pair(null, th1), Pair(th1, th2), Pair(th2, th3))) {
            cs.connect(curr.id, ConstraintSet.START, main.id, ConstraintSet.END)
            cs.connect(curr.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
            cs.constrainWidth(curr.id, 0)
            cs.constrainHeight(curr.id, 0)
            if (prev == null) {
                cs.connect(curr.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            } else {
                cs.connect(curr.id, ConstraintSet.TOP, prev.id, ConstraintSet.BOTTOM)
            }
        }
        cs.connect(th3.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)

        cs.createHorizontalChain(
            ConstraintSet.PARENT_ID, ConstraintSet.TOP,
            ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM,
            intArrayOf(th1.id, th2.id, th3.id), null,
            ConstraintSet.CHAIN_SPREAD_INSIDE
        )
    }

    // ──────────────────────────────────────────────────────────
    // Modo 7 — Grade 2×2 (todos iguais)
    // ──────────────────────────────────────────────────────────
    private fun constraintGrid2x2(cs: ConstraintSet) {
        val tl = playerViews[0] // top-left
        val tr = playerViews[1] // top-right
        val bl = playerViews[2] // bottom-left
        val br = playerViews[3] // bottom-right

        cs.setVisibility(tl.id, ConstraintSet.VISIBLE)
        cs.setVisibility(tr.id, ConstraintSet.VISIBLE)
        cs.setVisibility(bl.id, ConstraintSet.VISIBLE)
        cs.setVisibility(br.id, ConstraintSet.VISIBLE)

        // Top-left
        cs.connect(tl.id, ConstraintSet.TOP,    ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        cs.connect(tl.id, ConstraintSet.BOTTOM, bl.id,                   ConstraintSet.TOP)
        cs.connect(tl.id, ConstraintSet.START,  ConstraintSet.PARENT_ID, ConstraintSet.START)
        cs.connect(tl.id, ConstraintSet.END,    tr.id,                   ConstraintSet.START)
        cs.constrainWidth(tl.id, 0); cs.constrainHeight(tl.id, 0)

        // Top-right
        cs.connect(tr.id, ConstraintSet.TOP,    ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        cs.connect(tr.id, ConstraintSet.BOTTOM, br.id,                   ConstraintSet.TOP)
        cs.connect(tr.id, ConstraintSet.START,  tl.id,                   ConstraintSet.END)
        cs.connect(tr.id, ConstraintSet.END,    ConstraintSet.PARENT_ID, ConstraintSet.END)
        cs.constrainWidth(tr.id, 0); cs.constrainHeight(tr.id, 0)

        // Bottom-left
        cs.connect(bl.id, ConstraintSet.TOP,    tl.id,                   ConstraintSet.BOTTOM)
        cs.connect(bl.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        cs.connect(bl.id, ConstraintSet.START,  ConstraintSet.PARENT_ID, ConstraintSet.START)
        cs.connect(bl.id, ConstraintSet.END,    br.id,                   ConstraintSet.START)
        cs.constrainWidth(bl.id, 0); cs.constrainHeight(bl.id, 0)

        // Bottom-right
        cs.connect(br.id, ConstraintSet.TOP,    tr.id,                   ConstraintSet.BOTTOM)
        cs.connect(br.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        cs.connect(br.id, ConstraintSet.START,  bl.id,                   ConstraintSet.END)
        cs.connect(br.id, ConstraintSet.END,    ConstraintSet.PARENT_ID, ConstraintSet.END)
        cs.constrainWidth(br.id, 0); cs.constrainHeight(br.id, 0)

        // Correntes horizontais e verticais para distribuição igual
        cs.createVerticalChain(
            ConstraintSet.PARENT_ID, ConstraintSet.TOP,
            ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM,
            intArrayOf(tl.id, bl.id), null, ConstraintSet.CHAIN_SPREAD_INSIDE
        )
        cs.createVerticalChain(
            ConstraintSet.PARENT_ID, ConstraintSet.TOP,
            ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM,
            intArrayOf(tr.id, br.id), null, ConstraintSet.CHAIN_SPREAD_INSIDE
        )
    }

    // ──────────────────────────────────────────────────────────
    // Modo 8 — Layout em T (player1 cima-esq + player2 cima-dir + player3 baixo)
    // ──────────────────────────────────────────────────────────
    private fun constraintTLayout(cs: ConstraintSet) {
        val topLeft  = playerViews[0]
        val topRight = playerViews[1]
        val bottom   = playerViews[2]

        cs.setVisibility(topLeft.id,  ConstraintSet.VISIBLE)
        cs.setVisibility(topRight.id, ConstraintSet.VISIBLE)
        cs.setVisibility(bottom.id,   ConstraintSet.VISIBLE)

        // Top-left — metade superior esquerda
        cs.connect(topLeft.id, ConstraintSet.TOP,    ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        cs.connect(topLeft.id, ConstraintSet.BOTTOM, bottom.id,               ConstraintSet.TOP)
        cs.connect(topLeft.id, ConstraintSet.START,  ConstraintSet.PARENT_ID, ConstraintSet.START)
        cs.connect(topLeft.id, ConstraintSet.END,    topRight.id,             ConstraintSet.START)
        cs.constrainWidth(topLeft.id, 0); cs.constrainHeight(topLeft.id, 0)
        cs.constrainPercentHeight(topLeft.id, 0.5f)

        // Top-right — metade superior direita
        cs.connect(topRight.id, ConstraintSet.TOP,    ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        cs.connect(topRight.id, ConstraintSet.BOTTOM, bottom.id,               ConstraintSet.TOP)
        cs.connect(topRight.id, ConstraintSet.START,  topLeft.id,              ConstraintSet.END)
        cs.connect(topRight.id, ConstraintSet.END,    ConstraintSet.PARENT_ID, ConstraintSet.END)
        cs.constrainWidth(topRight.id, 0); cs.constrainHeight(topRight.id, 0)
        cs.constrainPercentHeight(topRight.id, 0.5f)

        // Bottom — largura total, metade inferior
        cs.connect(bottom.id, ConstraintSet.TOP,    topLeft.id,              ConstraintSet.BOTTOM)
        cs.connect(bottom.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        cs.connect(bottom.id, ConstraintSet.START,  ConstraintSet.PARENT_ID, ConstraintSet.START)
        cs.connect(bottom.id, ConstraintSet.END,    ConstraintSet.PARENT_ID, ConstraintSet.END)
        cs.constrainWidth(bottom.id, 0); cs.constrainHeight(bottom.id, 0)
        cs.constrainPercentHeight(bottom.id, 0.5f)
    }
}

// ──────────────────────────────────────────────────────────
// Enum dos 8 modos de layout
// ──────────────────────────────────────────────────────────

/**
 * Os oito modos de visualização suportados pelo DualStreamPlayer Pro.
 *
 * @param labelRes  String resource para o nome do modo (exibido na UI)
 * @param iconRes   Drawable resource do ícone para a barra de layouts
 * @param playerCount Quantos players são usados neste modo
 */
enum class LayoutMode(
    val labelRes: Int,
    val playerCount: Int
) {
    /** Modo 1 — Tela única */
    SINGLE(R.string.layout_single, 1),

    /** Modo 2 — Divisão horizontal */
    HORIZONTAL_SPLIT(R.string.layout_horizontal, 2),

    /** Modo 3 — Divisão vertical */
    VERTICAL_SPLIT(R.string.layout_vertical, 2),

    /** Modo 4 — Picture in Picture */
    PIP(R.string.layout_pip, 2),

    /** Modo 5 — Principal + 2 miniaturas */
    MAIN_PLUS_2(R.string.layout_main_plus_2, 3),

    /** Modo 6 — Principal + 3 miniaturas */
    MAIN_PLUS_3(R.string.layout_main_plus_3, 4),

    /** Modo 7 — Grade 2×2 */
    GRID_2X2(R.string.layout_grid_2x2, 4),

    /** Modo 8 — Layout em T */
    T_LAYOUT(R.string.layout_t, 3);
}
