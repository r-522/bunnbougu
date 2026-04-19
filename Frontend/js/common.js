'use strict';

/*
 * common.js
 * 文房具基幹システム 共通JavaScript
 * ゼロJS思想に基づき、最小限のユーティリティのみ提供する。
 * - グローバル汚染禁止: 即時関数（IIFE）で隔離
 * - var 禁止、const / let のみ使用
 * - 厳密比較（===）のみ使用
 */

(function () {
  // 数値フォーマット：3桁カンマ区切り
  // 引数 value: 数値または数値文字列
  // 戻り値: カンマ区切り文字列（NaN時は元の値をそのまま返却）
  const formatNumber = function (value) {
    // 数値化を試みる
    const n = Number(value);
    // NaNの場合は元値をそのまま返却（業務要件：壊れたデータを隠蔽しない）
    if (Number.isNaN(n)) {
      return String(value);
    }
    // toLocaleString でカンマ区切りを適用
    return n.toLocaleString('ja-JP');
  };

  // DOMContentLoaded時に data-format="number" 付与要素を一括フォーマット
  document.addEventListener('DOMContentLoaded', function () {
    // querySelectorAll で対象要素を列挙
    const nodes = document.querySelectorAll('[data-format="number"]');
    // for...of で個別にフォーマット適用
    for (const node of nodes) {
      // textContent を取得し、フォーマット後に再代入
      node.textContent = formatNumber(node.textContent.trim());
    }
  });

  // 削除リンクの確認ダイアログ（data-confirm属性のあるフォームで起動）
  document.addEventListener('submit', function (ev) {
    // submitされたform要素
    const form = ev.target;
    // data-confirm属性のメッセージを取得
    const msg = form.getAttribute('data-confirm');
    // 属性がない場合は通常処理を継続
    if (msg === null) {
      return;
    }
    // confirm で確認、キャンセル時は送信を中断
    if (!window.confirm(msg)) {
      ev.preventDefault();
    }
  });
})();
