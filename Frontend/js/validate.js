'use strict';

/*
 * validate.js
 * 画面共通の入力バリデーション（クライアント側の必須チェック・書式チェックのみ）
 * サーバー側でも同等のチェックを必ず実施する前提。
 */

(function () {
  // 社員番号バリデーション：5桁半角数字のみ
  // 引数 value: 入力文字列
  // 戻り値: true=正常、false=不正
  const isValidSyainNo = function (value) {
    // 正規表現で5桁の数字のみを許容
    return /^\d{5}$/.test(value);
  };

  // 必須項目チェック：空文字・空白のみを不正とする
  const isNotBlank = function (value) {
    // null/undefinedも空扱い
    if (value === null || value === undefined) {
      return false;
    }
    // 前後空白を除去して長さ判定
    return String(value).trim().length > 0;
  };

  // 数値チェック：半角数字のみ（小数点許容・マイナス許容）
  const isNumeric = function (value) {
    // 空文字は別途必須チェックで判定するためここではtrue
    if (value === '') {
      return true;
    }
    return /^-?\d+(\.\d+)?$/.test(value);
  };

  // エラーメッセージ表示：data-error-for="<入力name>"要素にメッセージを描画
  // 引数 form: form要素, name: 入力要素のname属性, msg: 表示メッセージ
  const showError = function (form, name, msg) {
    // 指定name属性のエラー表示用要素を取得
    const target = form.querySelector('[data-error-for="' + name + '"]');
    // 対象が存在する場合のみテキスト設定
    if (target !== null) {
      target.textContent = msg;
    }
  };

  // 全エラーメッセージをクリア
  const clearErrors = function (form) {
    // data-error-for属性を持つ全要素を取得
    const nodes = form.querySelectorAll('[data-error-for]');
    // for...of でループしてテキストを空にする
    for (const node of nodes) {
      node.textContent = '';
    }
  };

  // ログインフォーム専用バリデーション
  // form要素のid="form-login"に対してsubmitイベントを監視
  document.addEventListener('DOMContentLoaded', function () {
    // ログインフォーム要素
    const loginForm = document.getElementById('form-login');
    // ログイン画面以外では処理不要のためreturn
    if (loginForm === null) {
      return;
    }
    // submitイベントハンドラを登録
    loginForm.addEventListener('submit', function (ev) {
      // 既存エラー表示をクリア
      clearErrors(loginForm);
      // 社員番号入力値
      const syainNo = loginForm.elements.namedItem('syain_no');
      // null安全：要素取得失敗時は処理中断
      if (syainNo === null) {
        return;
      }
      // 入力値を取得しtrim
      const val = syainNo.value.trim();
      // 必須チェック
      if (!isNotBlank(val)) {
        // 送信を中断してエラー表示
        ev.preventDefault();
        showError(loginForm, 'syain_no', '社員番号を入力してください。');
        return;
      }
      // 書式チェック（5桁数字）
      if (!isValidSyainNo(val)) {
        // 送信を中断してエラー表示
        ev.preventDefault();
        showError(loginForm, 'syain_no', '社員番号は5桁の半角数字で入力してください。');
        return;
      }
    });
  });

  // 汎用必須チェック：form要素に data-validate="required" を付与した入力を対象
  document.addEventListener('submit', function (ev) {
    // submit対象のform要素
    const form = ev.target;
    // form要素でなければ何もしない
    if (!(form instanceof HTMLFormElement)) {
      return;
    }
    // data-validate="required"属性を持つ入力を取得
    const requiredInputs = form.querySelectorAll('[data-validate~="required"]');
    // ループして必須チェック
    for (const input of requiredInputs) {
      // 値が空の場合はエラー
      if (!isNotBlank(input.value)) {
        // 送信を中断
        ev.preventDefault();
        // エラー表示
        showError(form, input.name, '入力してください。');
        // 先頭のエラー要素にフォーカス
        input.focus();
        return;
      }
    }
    // 数値チェック
    const numericInputs = form.querySelectorAll('[data-validate~="numeric"]');
    for (const input of numericInputs) {
      if (!isNumeric(input.value.trim())) {
        ev.preventDefault();
        showError(form, input.name, '半角数値で入力してください。');
        input.focus();
        return;
      }
    }
  });
})();
