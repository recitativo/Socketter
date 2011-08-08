package socketter;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jetty.websocket.WebSocket;

import twitter4j.DirectMessage;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.User;
import twitter4j.UserList;
import twitter4j.UserStream;
import twitter4j.UserStreamAdapter;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;
import twitter4j.http.AccessToken;
import twitter4j.http.RequestToken;

public class WebSocketTwitter implements WebSocket {

	private Outbound outbound_;
	private static Set<WebSocketTwitter> connections_ = new CopyOnWriteArraySet<WebSocketTwitter>();
	private TwitterStream twitterStream;
	private UserStream userStream;
	private Thread th;
	private Boolean thstop;

	private Twitter tw;
	private RequestToken rt;
	private AccessToken at;
	private String consumerKey = "5L96A2mk84eChzlgmu1w";
	private String consumerSecret = "OuXFaS8KXkkSnbEc3kZGkJkUoXERyexe3XFtAcbGjbQ";
	private String accessToken = "";
	private String accessTokenSecret = "";
	private String screenName = "";

	public WebSocketTwitter(){
	}
	
	public Configuration OAuthConfig(){
		ConfigurationBuilder config = new ConfigurationBuilder();
		config.setOAuthConsumerKey(consumerKey);
		config.setOAuthConsumerSecret(consumerSecret);
		config.setOAuthAccessToken(accessToken);
		config.setOAuthAccessTokenSecret(accessTokenSecret);
		return config.build();
	}

	public AccessToken getAccessToken(){
		return new AccessToken(accessToken, accessTokenSecret);
	}

	public Twitter getTwitter(){
		return new TwitterFactory(OAuthConfig()).getInstance(getAccessToken());
	}
	public void startTwitterUserStream(){
		thstop = false;
		th = new Thread(){
			public void run(){
				TwitterStreamFactory factory = new TwitterStreamFactory(OAuthConfig());
				twitterStream = factory.getInstance(getAccessToken());
				twitterStream.addListener(userStreamAdapter);
				System.out.println("Start Twitter " + screenName);
				try {
					userStream = twitterStream.getUserStream();
					while(true){
						if(thstop) break;
						userStream.next(userStreamAdapter);
					}
				} catch (TwitterException e) {
					e.printStackTrace();
				}
				System.out.println("Stop Twitter " + screenName);
			}
		};
		
		th.start();
	}
	  @Override
	public void onConnect(Outbound outbound) {
		thstop = true;
		outbound_ = outbound;
		connections_.add(this);
		System.out.println("onConnect " + connections_.size() + "users");
	}

	@Override
	public void onDisconnect() {
		thstop = true;
		connections_.remove(this);
		System.out.println("onDisconnect " + screenName);
	}

	@Override
	public void onMessage(byte frame, String data) {
		Pattern pp = Pattern.compile("^twitter[ ]*\\([ ]*([0-9]+)[ ]*\\)$");
		Matcher mp = pp.matcher(data);
		Pattern pa = Pattern.compile("^twitter[ ]*\\([ ]*([a-zA-Z0-9\\-]+)[ ]*,[ ]*([a-zA-Z0-9]+)[ ]*\\)$");
		Matcher ma = pa.matcher(data);
		Pattern pt = Pattern.compile("^tweet[ ]*\\([ ]*(.+)[ ]*\\)$");
		Matcher mt = pt.matcher(data);
		if(data.equals("twitter")) {
			// show oauth URL
			String url = "";
			tw = new TwitterFactory().getInstance();
			tw.setOAuthConsumer(consumerKey, consumerSecret);
			try {
				rt = tw.getOAuthRequestToken();
				url = rt.getAuthorizationURL();
				outbound_.sendMessage("下記URLに行ってtwitterで認証して暗証番号をゲットしてきて\n" + url + "\nそんで twitter(暗証番号) を入力して");
			} catch (TwitterException e) {
				try {
					outbound_.sendMessage("認証用URLの取得に失敗");
				} catch (IOException e1) {
//					e1.printStackTrace();
				}
			} catch (IOException e) {
//			e.printStackTrace();
			}

		} else if(mp.find()){
			// get pin code
			String pin = mp.group(1);

			// get access token
			try {
				at = tw.getOAuthAccessToken(rt, pin);
				accessToken = at.getToken();
				accessTokenSecret = at.getTokenSecret();
				outbound_.sendMessage("以下は AccessToken と AccessTokenSecret だよ\nAccessToken : " + accessToken + "\nAccessTokenSecret : " + accessTokenSecret + "\nこんどから下記コマンドだけでtwitterを開始できるよ\ntwitter(" + accessToken + "," + accessTokenSecret + ")");
			} catch (TwitterException e) {
				e.printStackTrace();
				try {
					outbound_.sendMessage("AccessToken取得失敗");
				} catch (IOException e1) {
//					e1.printStackTrace();
				}
			} catch (IOException e) {
//				e.printStackTrace();
			}
		} else if(ma.find()){
			// get access token
			accessToken = ma.group(1);
			accessTokenSecret = ma.group(2);
			thstop = true;

			tw = getTwitter();
			try {
				screenName = tw.verifyCredentials().getScreenName();
			} catch (TwitterException e) {
				e.printStackTrace();
			}

			startTwitterUserStream();
		} else if(mt.find()){
			// tweet
			if(tw == null || !tw.isOAuthEnabled()){
				tw = getTwitter();
			}
			try {
				tw.updateStatus(mt.group(1));
			} catch (TwitterException e) {
				e.printStackTrace();
			}
		} else {
			// チャットメッセージ
			for(WebSocketTwitter connection : connections_) {
				try {
					connection.outbound_.sendMessage(frame, data);
				} catch (IOException e) {
//					e.printStackTrace();
				}
			}
		}
		System.out.println("onMessage " + screenName);
	}

	@Override
	public void onMessage(byte frame, byte[] data, int offset, int length) {
		for(WebSocketTwitter connection : connections_) {
			try {
				connection.outbound_.sendMessage(frame, data, offset, length);
			} catch (IOException e) {
//				e.printStackTrace();
			}
		}
		System.out.println("onMessage2 " + screenName);
	}

	// 以下、twitter4jのUserStreamListener
	public UserStreamAdapter userStreamAdapter = new UserStreamAdapter(){ 
		@Override
		public void onDeletionNotice(StatusDeletionNotice arg0) {
			System.out.println("onDeletionNotice");
		}
	
		@Override
		public void onScrubGeo(int arg0, long arg1) {
			System.out.println("onScrubGeo");
		}
	
		@Override
		public void onStatus(Status status) {
			try {
				String datetime = Integer.toString(status.getCreatedAt().getYear() + 1900) + "/";
				datetime += Integer.toString(status.getCreatedAt().getMonth() + 1) + "/";
				datetime += Integer.toString(status.getCreatedAt().getDate()) + " ";
				datetime += Integer.toString(status.getCreatedAt().getHours()) + ":";
				datetime += Integer.toString(status.getCreatedAt().getMinutes()) + ":";
				datetime += Integer.toString(status.getCreatedAt().getSeconds());
				outbound_.sendMessage(status.getUser().getScreenName() + " : " + datetime + " : " + status.getText());
			} catch (IOException e) {
//					e.printStackTrace();
			}
			System.out.println("onStatus " + screenName);
		}
	
		@Override
		public void onTrackLimitationNotice(int arg0) {
			System.out.println("onTrackLimitationNotice " + screenName);
		}
	
		@Override
		public void onException(Exception arg0) {
			System.out.println("onException " + screenName);
		}
	
		@Override
		public void onBlock(User arg0, User arg1) {
			System.out.println("onBlock " + screenName);
		}
	
		@Override
		public void onDeletionNotice(long arg0, int arg1) {
			System.out.println("onDeletionNotice " + screenName);
		}
	
		@Override
		public void onDirectMessage(DirectMessage arg0) {
			System.out.println("onDirectMessage " + screenName);
		}
	
		@Override
		public void onFavorite(User arg0, User arg1, Status arg2) {
			System.out.println("onFavorite " + screenName);
		}
	
		@Override
		public void onFollow(User arg0, User arg1) {
			System.out.println("onFollow " + screenName);
		}
	
		@Override
		public void onFriendList(int[] arg0) {
			System.out.println("onFriendList " + screenName);
		}
	
		@Override
		public void onRetweet(User arg0, User arg1, Status arg2) {
			System.out.println("onRetweet " + screenName);
		}
	
		@Override
		public void onUnblock(User arg0, User arg1) {
			System.out.println("onUnblock " + screenName);
		}
	
		@Override
		public void onUnfavorite(User arg0, User arg1, Status arg2) {
			System.out.println("onUnfavorite " + screenName);
		}
	
		@Override
		public void onUserListCreation(User arg0, UserList arg1) {
			System.out.println("onUserListCreation " + screenName);
		}
	
		@Override
		public void onUserListDeletion(User arg0, UserList arg1) {
			System.out.println("onUserListDeletion " + screenName);
		}
	
		@Override
		public void onUserListMemberAddition(User arg0, User arg1, UserList arg2) {
			System.out.println("onUserListMemberAddition " + screenName);
		}
	
		@Override
		public void onUserListMemberDeletion(User arg0, User arg1, UserList arg2) {
			System.out.println("onUserListMemberDeletion " + screenName);
		}
	
		@Override
		public void onUserListSubscription(User arg0, User arg1, UserList arg2) {
			System.out.println("onUserListSubscription " + screenName);
		}
	
		@Override
		public void onUserListUnsubscription(User arg0, User arg1, UserList arg2) {
			System.out.println("onUserListUnsubscription " + screenName);
		}
	
		@Override
		public void onUserListUpdate(User arg0, UserList arg1) {
			System.out.println("onUserListUpdate " + screenName);
		}
	
		@Override
		public void onUserProfileUpdate(User arg0) {
			System.out.println("onUserProfileUpdate " + screenName);
		}
	};
}
