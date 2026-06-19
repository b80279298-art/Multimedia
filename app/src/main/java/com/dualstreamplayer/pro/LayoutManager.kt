package com.dualstreamplayer.pro

import android.util.Log
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import androidx.media3.ui.PlayerView

/**
 * LayoutManager gerencia os 8 modos de visualização disponíveis,
 * reorganizando os quatro PlayerViews dentro do ConstraintLayout
 * com animações suaves de transição (ChangeBounds).
 *
 * Estratégia de constraints:
 *  Todos os modos usam constraintPercent* com bias para posicionamento
 *  absoluto e previsível, sem createHorizontalChain / createVerticalChain,
 *  que exigem setup bidirecional complexo e são fontes comuns de bugs.
 */
class LayoutManager(
    private val container: ConstraintLayout,
    private val playerViews: List<PlayerView>
) {

    companion object {
        private const val TAG = "LayoutManager"
        private const val TRANSITION_MS = 300L
    }

    var currentMode: LayoutMode = LayoutMode.SINGLE
        private set

    // ──────────────────────────────────────────────────────────
    // API pública
    // ──────────────────────────────────────────────────────────

    fun applyLayout(mode: LayoutMode) {
        if (mode == currentMode) return
        Log.d(TAG, "Layout: $currentMode → $mode")
        currentMode = mode
        val transition = ChangeBounds().apply { duration = TRANSITION_MS }
        TransitionManager.beginDelayedTransition(container, transition)
        val cs = ConstraintSet().also { it.clone(container) }
        // Esconder tudo primeiro
        playerViews.forEach { cs.setVisibility(it.id, ConstraintSet.GONE) }
        when (mode) {
            LayoutMode.SINGLE           -> layoutSingle(cs)
            LayoutMode.HORIZONTAL_SPLIT -> layoutHorizontalSplit(cs)
            LayoutMode.VERTICAL_SPLIT   -> layoutVerticalSplit(cs)
            LayoutMode.PIP              -> layoutPip(cs)
            LayoutMode.MAIN_PLUS_2      -> layoutMainPlus2(cs)
            LayoutMode.MAIN_PLUS_3      -> layoutMainPlus3(cs)
            LayoutMode.GRID_2X2         -> layoutGrid2x2(cs)
            LayoutMode.T_LAYOUT         -> layoutTLayout(cs)
        }
        cs.applyTo(container)
    }

    // ──────────────────────────────────────────────────────────
    // Utilitário: preenche o pai inteiro com o view indicado
    // ──────────────────────────────────────────────────────────
    private fun fullParent(cs: ConstraintSet, id: Int) {
        cs.connect(id, ConstraintSet.TOP,    ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        cs.connect(id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        cs.connect(id, ConstraintSet.START,  ConstraintSet.PARENT_ID, ConstraintSet.START)
        cs.connect(id, ConstraintSet.END,    ConstraintSet.PARENT_ID, ConstraintSet.END)
        cs.constrainWidth(id, 0)
        cs.constrainHeight(id, 0)
        cs.setVisibility(id, ConstraintSet.VISIBLE)
    }

    /**
     * Posiciona o view como uma fatia percentual do pai.
     *
     * @param wPct   largura como fração [0,1]; null = 0dp (fill)
     * @param hPct   altura como fração [0,1]; null = 0dp (fill)
     * @param hBias  bias horizontal [0=esquerda, 1=direita]
     * @param vBias  bias vertical   [0=topo,     1=base]
     */
    private fun percentSlot(
        cs: ConstraintSet, id: Int,
        wPct: Float?, hPct: Float?,
        hBias: Float = 0.5f, vBias: Float = 0.5f
    ) {
        cs.connect(id, ConstraintSet.TOP,    ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        cs.connect(id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        cs.connect(id, ConstraintSet.START,  ConstraintSet.PARENT_ID, ConstraintSet.START)
        cs.connect(id, ConstraintSet.END,    ConstraintSet.PARENT_ID, ConstraintSet.END)
        cs.constrainWidth(id, 0)
        cs.constrainHeight(id, 0)
        if (wPct != null) cs.constrainPercentWidth(id, wPct)
        if (hPct != null) cs.constrainPercentHeight(id, hPct)
        cs.setHorizontalBias(id, hBias)
        cs.setVerticalBias(id, vBias)
        cs.setVisibility(id, ConstraintSet.VISIBLE)
    }

    // ──────────────────────────────────────────────────────────
    // Modo 1 — Tela única
    // ──────────────────────────────────────────────────────────
    private fun layoutSingle(cs: ConstraintSet) {
        fullParent(cs, playerViews[0].id)
    }

    // ──────────────────────────────────────────────────────────
    // Modo 2 — Divisão horizontal (player1 topo | player2 base)
    // ──────────────────────────────────────────────────────────
    private fun layoutHorizontalSplit(cs: ConstraintSet) {
        // player1: 100 % largura, 50 % altura → topo (vBias=0)
        percentSlot(cs, playerViews[0].id, wPct = null, hPct = 0.5f, vBias = 0f)
        // player2: 100 % largura, 50 % altura → base (vBias=1)
        percentSlot(cs, playerViews[1].id, wPct = null, hPct = 0.5f, vBias = 1f)
    }

    // ──────────────────────────────────────────────────────────
    // Modo 3 — Divisão vertical (player1 esq | player2 dir)
    // ──────────────────────────────────────────────────────────
    private fun layoutVerticalSplit(cs: ConstraintSet) {
        // player1: 50 % largura, 100 % altura → esquerda (hBias=0)
        percentSlot(cs, playerViews[0].id, wPct = 0.5f, hPct = null, hBias = 0f)
        // player2: 50 % largura, 100 % altura → direita (hBias=1)
        percentSlot(cs, playerViews[1].id, wPct = 0.5f, hPct = null, hBias = 1f)
    }

    // ──────────────────────────────────────────────────────────
    // Modo 4 — Picture in Picture (player1 full + player2 mini)
    // ──────────────────────────────────────────────────────────
    private fun layoutPip(cs: ConstraintSet) {
        // player1 — tela cheia
        fullParent(cs, playerViews[0].id)

        // player2 — mini no canto inferior direito: 25 % × 25 %, bias (1,1)
        percentSlot(cs, playerViews[1].id, wPct = 0.25f, hPct = 0.25f, hBias = 0.97f, vBias = 0.95f)
        cs.setElevation(playerViews[1].id, 16f)
    }

    // ──────────────────────────────────────────────────────────
    // Modo 5 — Principal + 2 miniaturas
    //   player1: 67 % largura, 100 % altura, hBias=0
    //   player2: 33 % largura, 50 % altura,  canto superior direito
    //   player3: 33 % largura, 50 % altura,  canto inferior direito
    // ──────────────────────────────────────────────────────────
    private fun layoutMainPlus2(cs: ConstraintSet) {
        percentSlot(cs, playerViews[0].id, wPct = 0.67f, hPct = null, hBias = 0f)
        percentSlot(cs, playerViews[1].id, wPct = 0.33f, hPct = 0.5f, hBias = 1f, vBias = 0f)
        percentSlot(cs, playerViews[2].id, wPct = 0.33f, hPct = 0.5f, hBias = 1f, vBias = 1f)
    }

    // ──────────────────────────────────────────────────────────
    // Modo 6 — Principal + 3 miniaturas
    //   player1: 65 % largura, 100 % altura, hBias=0
    //   player2: 35 % largura, 33 % altura,  coluna direita topo
    //   player3: 35 % largura, 33 % altura,  coluna direita centro
    //   player4: 35 % largura, 34 % altura,  coluna direita base
    // ──────────────────────────────────────────────────────────
    private fun layoutMainPlus3(cs: ConstraintSet) {
        percentSlot(cs, playerViews[0].id, wPct = 0.65f, hPct = null, hBias = 0f)
        percentSlot(cs, playerViews[1].id, wPct = 0.35f, hPct = 0.333f, hBias = 1f, vBias = 0f)
        percentSlot(cs, playerViews[2].id, wPct = 0.35f, hPct = 0.333f, hBias = 1f, vBias = 0.5f)
        percentSlot(cs, playerViews[3].id, wPct = 0.35f, hPct = 0.334f, hBias = 1f, vBias = 1f)
    }

    // ──────────────────────────────────────────────────────────
    // Modo 7 — Grade 2×2
    //   tl(0): 50 % × 50 %, hBias=0, vBias=0
    //   tr(1): 50 % × 50 %, hBias=1, vBias=0
    //   bl(2): 50 % × 50 %, hBias=0, vBias=1
    //   br(3): 50 % × 50 %, hBias=1, vBias=1
    // ──────────────────────────────────────────────────────────
    private fun layoutGrid2x2(cs: ConstraintSet) {
        percentSlot(cs, playerViews[0].id, wPct = 0.5f, hPct = 0.5f, hBias = 0f, vBias = 0f)
        percentSlot(cs, playerViews[1].id, wPct = 0.5f, hPct = 0.5f, hBias = 1f, vBias = 0f)
        percentSlot(cs, playerViews[2].id, wPct = 0.5f, hPct = 0.5f, hBias = 0f, vBias = 1f)
        percentSlot(cs, playerViews[3].id, wPct = 0.5f, hPct = 0.5f, hBias = 1f, vBias = 1f)
    }

    // ──────────────────────────────────────────────────────────
    // Modo 8 — Layout em T
    //   player1: 50 % largura, 50 % altura, topo-esquerda
    //   player2: 50 % largura, 50 % altura, topo-direita
    //   player3: 100% largura, 50 % altura, base
    // ──────────────────────────────────────────────────────────
    private fun layoutTLayout(cs: ConstraintSet) {
        percentSlot(cs, playerViews[0].id, wPct = 0.5f, hPct = 0.5f, hBias = 0f, vBias = 0f)
        percentSlot(cs, playerViews[1].id, wPct = 0.5f, hPct = 0.5f, hBias = 1f, vBias = 0f)
        percentSlot(cs, playerViews[2].id, wPct = null, hPct = 0.5f, hBias = 0.5f, vBias = 1f)
    }
}

// ──────────────────────────────────────────────────────────
// Enum dos 8 modos de layout
// ──────────────────────────────────────────────────────────

enum class LayoutMode(val labelRes: Int, val playerCount: Int) {
    SINGLE(R.string.layout_single, 1),
    HORIZONTAL_SPLIT(R.string.layout_horizontal, 2),
    VERTICAL_SPLIT(R.string.layout_vertical, 2),
    PIP(R.string.layout_pip, 2),
    MAIN_PLUS_2(R.string.layout_main_plus_2, 3),
    MAIN_PLUS_3(R.string.layout_main_plus_3, 4),
    GRID_2X2(R.string.layout_grid_2x2, 4),
    T_LAYOUT(R.string.layout_t, 3);
}
