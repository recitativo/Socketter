//グローバル変数にWebSocketの変数を定義
var wscssl;

var msgMode = "text";

var accesstoken = "";
var accesstokensecret = "";

//getElementByIdの別名を定義
function $(id){
	return document.getElementById(id);
}

//WebSocketが接続された時
function onOpenWebSocket(){
	$("status").innerHTML = "connected";
}

//WebSocketが切断された時
function onCloseWebSocket(){
	$("status").innerHTML = "disconnected";
}

//ウィンドウを閉じたり画面遷移した時にWebSokcetを切断する
function onUnloadWebSocket(){
	wscssl.close();
}

function onOnlineWebSocket(){
	wsClose();
	setTimeout("wsOpen()", 500);
}

function onOfflineWebSocket(){
	wsClose();
	$("status").innerHTML = "offline";
}

function wsReconnect(){
	wsClose();
	if(accesstoken != ""){
		sendMsg("twitter(" + accesstoken + "," + accesstokensecret + ")");
	}else{
		setTimeout("wsOpen()", 500);
	}
}

function wsHeartBeat(){
//	wsClose();
//	setTimeout("wsOpen()", 500);
}

//接続先よりメッセージを受信した時に，空文字でなければ画面に表示する
function onMessageWebSocket(event){
	var msg = event.data;
	var re = new RegExp("^draw\\((#[a-f0-9]{6}),([0-9]+),([0-9]+),([0-9]+),([0-9]+),([0-9]+)\\)$");
	if(msg == "") return;
	else if(msg == "startCanvas()") startCanvas();
	else if(msg == "exitCanvas()") exitCanvas();
	else if(msg == "submitCanvas()") submitCanvas();
	else if(msg == "clearCanvas()") clearCanvas();
	else if(msg.match(re)){
		// sendMsg("draw(" + lineColor + "," + lineWidth + "," + fromX + "," + fromY + "," + toX + "," + toY")");
		drawLine(RegExp.$1, RegExp.$2, RegExp.$3, RegExp.$4, RegExp.$5, RegExp.$6);
	}
	else dispMessage("msg", msg);
}

//画面にメッセージを表示する
//上に表示されるメッセージが最新となる
function dispMessage(mode, msg){
	var ele = null;
	if(mode == "msg"){
		ele = document.createElement("pre");
		ele.setAttribute("class", "msg");
		ele.appendChild(document.createTextNode(msg));
	}else if(mode == "img"){
		ele = document.createElement("img");
		ele.setAttribute("style", "border:0px; display:block");
		ele.setAttribute("src", msg);
	}

	if($("messages").hasChildNodes()){
		$("messages").insertBefore(ele,$("messages").firstChild);
	}else{
		$("messages").appendChild(ele);
	}
}

//メッセージ入力欄が空白でなければメッセージを送信する
function sendMessage(ev){
	sendMsg($("message").value);
	$("message").value = "";

	// デフォルト動作禁止（form.onsubmitで画面切り替えしない）
	try{
		ev.stopPropagation();
		ev.preventDefault();
	} catch (e) {}

	return false;
}

function sendMsg(msg){
	if(msg == "help"){
		dispMessage("msg", "Socketter は、HTML5の勉強用に作ったチャット＋Twitterのアプリだよ。\nWebSocket+SSL、Canvas、SVG、などなど使っているよ。\n※Chromeで動くことしか確認してないにょｗｗｗｗ\n\n文字列\n\tチャットメッセージをブロードキャストする。\n\ntwitter\n\ttwitterのOAuth認証を行うためのURLを表示する。\n\ntwitter(暗証番号)\n\tOAuth認証後、取得した暗証番号を送信してアクセストークンを取得する。\n\ntwitter(AccessToken,AccessTokenSecret)\n\t取得したアクセストークンを利用してtwitterにアクセスする。\n\ntweet(文字列)\n\ttwitterにアクセス出来ていれば、文字列をtwitterに投稿する。");
	}else if(msg != ""){
		re = new RegExp("^twitter[ ]*\\(([a-zA-Z0-9\\-]+)[ ]*,[ ]*([a-zA-Z0-9]+)[ ]*\\)$");
		if(msg.match(re)){
			accesstoken = RegExp.$1;
			accesstokensecret = RegExp.$2;
		}
		if(!wscssl){
			wsOpen();
			setTimeout("lateSend(\"" + msg + "\")", 500);
		}else if(wscssl.readyState == 1){
			wscssl.send(msg);
		}else if(wscssl.readyState == 0){
			setTimeout("lateSend(\"" + msg + "\")", 500);
		}else{
			wsOpen();
			setTimeout("lateSend(\"" + msg + "\")", 500);
		}
	}
}

function lateSend(msg){
	if(!wscssl){
		wsOpen();
		setTimeout("lateSend(\"" + msg + "\")", 500);
	}else if(wscssl.readyState == 1){
		wscssl.send(msg);
	}else if(wscssl.readyState == 0){
		setTimeout("lateSend(\"" + msg + "\")", 500);
	}else{
		wsOpen();
		setTimeout("lateSend(\"" + msg + "\")", 500);
	}
}

// evalで評価する文字列（strEval）がtrueを返すまでループで待つ。ただし、timeOut(ミリ秒)まで。
function loopWait(strEval, timeOut){
	var timeStart = new Date().getTime();
	var timeNow = new Date().getTime();
	while(true){
		// 条件チェック
		var status = false;
		try{
			status = eval(strEval);
		} catch(e) {
			status = false;
		}

		// 条件が満たされた
		if(status) return true;
		// タイムアウト
		else if(timeNow >= (timeStart + timeOut)) return false;

		timeNow = new Date().getTime();
	}
}

// タイムアウトのみ
function loopWait(timeOut){
	var timeStart = new Date().getTime();
	var timeNow = new Date().getTime();
	while(true){
		// タイムアウト
		if(timeNow >= (timeStart + timeOut)) break;
		timeNow = new Date().getTime();
	}
}

function wsOpen(){
	try {
		if(wscssl == null || (wscssl.readyState != 1 && wscssl.readyState != 0)){
			//HTTPSで接続されている場合，WebSocketもセキュアにする
			var protocol = (location.protocol=="https:")?"wss":"ws";
		
			//port番号も込みで取得
			var host = location.host;
		
			//接続先URLの組み立て
			var url = protocol + "://" + host + "/ws/";
		
			//WebSocketのインスタンス化
			wscssl = null;
			
			if(window.MozWebSocket){
				wscssl = new MozWebSocket(url);
			}else{
				wscssl = new WebSocket(url);
			}
		
			//WebSocketのイベントの登録
			wscssl.addEventListener("open", onOpenWebSocket, false);
			wscssl.addEventListener("close", onCloseWebSocket, false);
			wscssl.addEventListener("message", onMessageWebSocket, false);
		}
	} catch (e){}
}

function wsClose(){
	wscssl.close();
	wscssl = null;
}

function chgMode(){
	$("msgarea").innerHTML = "";
	mark = "▼";
	disp = "inline";
	msg = null;
	if(msgMode == "text"){
		msgMode = "textarea";
		mark = "▲";
		msg = document.createElement(msgMode);
		msg.setAttribute("rows", "5");
	}else{
		msgMode = "text"
		disp = "none";
		msg = document.createElement("input");
		msg.setAttribute("input", "text");
	}
	msg.setAttribute("id", "message");
	msg.setAttribute("placeholder", "helpと入力してみよう");
	$("msgarea").appendChild(msg);
	$("sendbtn").style.display = disp;
	$("exp").innerHTML = "<a href=\"javascript:void(0)\" style=\"text-decoration:none;color:black\">" + mark + "</a>";
}

///////////////////////////////////////////////////////////////////////////////

var canvas = null;
var ctx = null;
var canvasW = 300;
var canvasH = 300;
var fromX;
var fromY;
var toX;
var toY;
var mouseDownFlag = false;	//マウスダウンフラグ
var lineWidth = 5;	//線の太さ
var lineColor = "#ff0000"; // 線の色

function draw() {
	//キャンバスの初期処理
	canvas = $("cvs");
	if ( ! canvas || ! canvas.getContext ) return false;
	//2Dコンテキスト
	ctx = canvas.getContext("2d");
	//イベント：マウス移動
	canvas.onmousemove = mouseMoveListner;
	function mouseMoveListner(e) {
		if (mouseDownFlag) {
			//座標調整
			adjustXY(e);
			
			// 線を描く
			//drawLine($("lineColor").value, lineWidth, fromX, fromY, toX, toY);
			sendMsg("draw(" + $("lineColor").value + "," + lineWidth + "," + Math.floor(fromX) + "," + Math.floor(fromY) + "," + Math.floor(toX) + "," + Math.floor(toY) + ")");

			// 位置を記録
			fromX = toX;
			fromY = toY;
		}
	}
	
	//イベント：マウスダウン
	canvas.onmousedown = mouseDownListner;
	function mouseDownListner(e) {
		mouseDownFlag = true;
		var rect = e.target.getBoundingClientRect();
		fromX = e.clientX - rect.left;
		fromY = e.clientY - rect.top;
	}
	
	//イベント：マウスアップ
	canvas.onmouseup = mouseUpListner;
	function mouseUpListner(e) {
		mouseDownFlag = false;
	}
	
	//イベント：マウスアウト
	canvas.onmouseout = mouseOutListner;
	function mouseOutListner(e) {
		mouseDownFlag = false;
	}

	//座標調整
	function adjustXY(e) {
		var rect = e.target.getBoundingClientRect();
		toX = e.clientX - rect.left;
		toY = e.clientY - rect.top;
	}
}

// 線を描く
function drawLine(color, width, fX, fY, tX, tY){
	// パスを初期化
	ctx.beginPath();
	// 線の端
	ctx.lineCap = "round";
	// 色指定
	ctx.strokeStyle = color;
	// 線の太さ
	ctx.lineWidth = width;
	// 前回記録された位置から
	ctx.moveTo(fX, fY);
	// 動いた位置まで
	ctx.lineTo(tX, tY);
	// パスを描く
	ctx.stroke();
}

function chgLineColor(){
	lineColor = $("lineColor").value;
	$("ln1").setAttribute("stroke", lineColor);
	$("ln3").setAttribute("stroke", lineColor)
	$("ln5").setAttribute("stroke", lineColor)
	$("ln7").setAttribute("stroke", lineColor)
	$("ln9").setAttribute("stroke", lineColor)
}

function startCanvas(){
	clearCanvas();
	canvas.setAttribute("width", canvasW);
	canvas.setAttribute("height", canvasH);
	
	$("lineColor").setAttribute("value", $("lineColor").value);
	$("bntLineColor").setAttribute("style", "background-color:" + $("lineColor").value);

	$("draw").style.height = canvasH + 10;
	$("draw").style.display = "block";
	$("pen").style.display = "none";
}

function exitCanvas(){
	$("draw").style.display = "none";
	$("pen").style.display = "inline";
}

function submitCanvas(){
	//キャンバスの初期処理
	if ( ! canvas || ! canvas.getContext ) return false;
	try {
		dispMessage("img", canvas.toDataURL());
	} catch(e) {
	}
}

function clearCanvas(){
	//キャンバスの初期処理
	if ( ! canvas || ! canvas.getContext ) return false;
	ctx.clearRect(0, 0, canvasW, canvasH);
}

function sendStartCanvas(){
	sendMsg("startCanvas()");
}

function sendExitCanvas(){
	sendMsg("exitCanvas()");
}

function sendSubmitCanvas(){
	sendMsg("submitCanvas()");
}

function sendClearCanvas(){
	sendMsg("clearCanvas()");
}

///////////////////////////////////////////////////////////////////////////////

//初期化処理
function initWSCSSL(){
	// 接続開始
	wsOpen();

	//ウィンドウを閉じたり画面遷移した時にWebSokcetを切断する
	window.addEventListener("unload", onUnloadWebSocket, false);
	window.addEventListener("online", onOnlineWebSocket, false);
	window.addEventListener("offline", onOfflineWebSocket, false);

	$("sendfrm").addEventListener("submit", sendMessage, false);

	$("exp").addEventListener("click", chgMode, false);
	
	$("pen").addEventListener("click", sendStartCanvas, false);

	// 接続確認開始
	setInterval("wsReconnect()", 300000);

	// canvas初期処理	
	draw();
}

//オンロード時のイベントに，初期化関数を定義
window.addEventListener("load", initWSCSSL, false);
