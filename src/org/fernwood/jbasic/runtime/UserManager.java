/*
 * THIS SOURCE FILE IS PART OF JBASIC, AN OPEN SOURCE PUBLICLY AVAILABLE
 * JAVA SOFTWARE PACKAGE HOSTED BY SOURCEFORGE.NET
 *
 * THIS SOFTWARE IS PROVIDED VIA THE GNU PUBLIC LICENSE AND IS FREELY
 * AVAILABLE FOR ANY PURPOSE COMMERCIAL OR OTHERWISE AS LONG AS THE AUTHORSHIP
 * AND COPYRIGHT INFORMATION IS RETAINED INTACT AND APPROPRIATELY VISIBLE
 * TO THE END USER.
 * 
 * SEE THE PROJECT FILE AT HTTP://WWW.SOURCEFORGE.NET/PROJECTS/JBASIC FOR
 * MORE INFORMATION.
 * 
 * COPYRIGHT 2003-2011 BY TOM COLE, TOMCOLE@USERS.SF.NET
 *
 * Created on Oct 12, 2007 by tom
 *
 */
package org.fernwood.jbasic.runtime;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;

import org.fernwood.jbasic.JBasic;
import org.fernwood.jbasic.Permissions;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.User;
import org.fernwood.jbasic.funcs.MkdirFunction;
import org.fernwood.jbasic.value.Value;

/**
 * @author tom
 * @version version 1.0 Oct 12, 2007
 *
 */
public class UserManager {

	private static final double	PW_UPCASE_FREQ	= 0.4;

	private static final int	PW_HYPH_FREQ	= 8;

	/**
	 * This is the list of words used to create passwords.
	 */
	static String[] xwordList = null;

	/**
	 * This is the secret key that is used to encode passwords.
	 */
	public static String secret = null;

	/*
	 * The XML file created to contain UserManager data uses this
	 * for the tag name.
	 */
	static String dictionaryXMLTag = "UserDictionary";
	
	/**
	 * This is the name of the physical file that contains the XML
	 * representation of the user manager database.
	 */
	private String databaseFileName;
	
	/**
	 * This is the map of user data objects, keyed by the user's name.
	 */
	private HashMap<String, User> users;
	
	/**
	 * This cipher object is used to handle password encryption.  Passwords
	 * are implemented as one-way hashes.
	 */
	SimpleCipher crypto;
	
	/**
	 * This is the controlling (hosting) session that is using this user manager
	 * to handle permissions.  This is usually the session that started up
	 * the multi-user server mode.
	 */
	private JBasic controllingSession;
	
	/**
	 * Is the physical database file automatically saved if the user database was
	 * modified since the last save?  If true, then the save is performed each time
	 * a modification occurs. If false, then the database is not automatically
	 * written and the save() method must be called explicitly in the controlling
	 * JBasic session (such as by a <code>SERVER SAVE</codes> command)
	 */
	private boolean autoSave;

	/**
	 * Storage area for the prompt string that was active before the
	 * server was started; this will be needed when the server is
	 * deactivated.
	 */
	public String savedPrompt;

	/**
	 * Has the user database in memory been modified in any way since
	 * the last SERVER SAVE operation?
	 */
	private boolean dirtyData;

	/**
	 * This is the vocabulary list used to create passwords
	 */
	private static String []wordlist = new String[] {
			"red","horse","blue","dog","black","sheep","coin", "stop",
			"secret","whisper","icon","pest","acorn","echo","free",
			"global","hot","joke","key","lose","mike","nest","open",
			"quid","thorn","uncle","vest","yarn","zebra",
			"ability","able","about","about","above","above","absence","absolutely",
			"academic","accept","access","accident","accompany","according","account",
			"account","achieve","achievement","acid","acquire","across","act","act",
			"action","active","activity","actual","actually","add","addition","additional",
			"address","address","administration","admit","adopt","adult","advance",
			"advantage","advice","advise","affair","affect","afford","afraid","after",
			"after","afternoon","afterwards","again","against","age","agency","agent",
			"ago","agree","agreement","ahead","aid","aim","aim","air","aircraft","all",
			"all","allow","almost","alone","alone","along","along","already","alright",
			"also","alternative","alternative","although","always","among","amongst",
			"amount","an","analysis","ancient","and","animal","announce","annual","another",
			"answer","answer","any","anybody","anyone","anything","anyway","apart",
			"apparent","apparently","appeal","appeal","appear","appearance","application",
			"apply","appoint","appointment","approach","approach","appropriate","approve",
			"area","argue","argument","arise","arm","army","around","around","arrange",
			"arrangement","arrive","art","article","artist","as","as","as","ask","aspect",
			"assembly","assess","assessment","asset","associate","association","assume",
			"assumption","at","atmosphere","attach","attack","attack","attempt","attempt",
			"attend","attention","attitude","attract","attractive","audience","author",
			"authority","available","average","avoid","award","award","aware","away",
			"aye","baby","back","back","background","bad","bag","balance","ball","band",
			"bank","bar","base","base","basic","basis","battle","be","bear","beat",
			"beautiful","because","become","bed","bedroom","before","before","before",
			"begin","beginning","behaviour","behind","belief","believe","belong","below",
			"below","beneath","benefit","beside","best","better","between","beyond",
			"big","bill","bind","bird","birth","bit","black","block","blood","bloody",
			"blow","blue","board","boat","body","bone","book","border","both","both",
			"bottle","bottom","box","boy","brain","branch","break","breath","bridge",
			"brief","bright","bring","broad","brother","budget","build","building",
			"burn","bus","business","busy","but","buy","by","cabinet","call","call",
			"campaign","can","candidate","capable","capacity","capital","car","card",
			"care","care","career","careful","carefully","carry","case","cash","cat",
			"catch","category","cause","cause","cell","central","centre","century",
			"certain","certainly","chain","chair","chairman","challenge","chance","change",
			"change","channel","chapter","character","characteristic","charge","charge",
			"cheap","check","chemical","chief","child","choice","choose","church","circle",
			"circumstance","citizen","city","civil","claim","claim","class","clean",
			"clear","clear","clearly","client","climb","close","close","close","closely",
			"clothes","club","coal","code","coffee","cold","colleague","collect","collection",
			"college","colour","combination","combine","come","comment","comment","commercial",
			"commission","commit","commitment","committee","common","communication",
			"community","company","compare","comparison","competition","complete","complete",
			"completely","complex","component","computer","concentrate","concentration",
			"concept","concern","concern","concerned","conclude","conclusion","condition",
			"conduct","conference","confidence","confirm","conflict","congress","connect",
			"connection","consequence","conservative","consider","considerable","consideration",
			"consist","constant","construction","consumer","contact","contact","contain",
			"content","context","continue","contract","contrast","contribute","contribution",
			"control","control","convention","conversation","copy","corner","corporate",
			"correct","cos","cost","cost","could","council","count","country","county",
			"couple","course","court","cover","cover","create","creation","credit",
			"crime","criminal","crisis","criterion","critical","criticism","cross",
			"crowd","cry","cultural","culture","cup","current","currently","curriculum",
			"customer","cut","cut","damage","damage","danger","dangerous","dark","data",
			"date","date","daughter","day","dead","deal","deal","death","debate","debt",
			"decade","decide","decision","declare","deep","deep","defence","defendant",
			"define","definition","degree","deliver","demand","demand","democratic",
			"demonstrate","deny","department","depend","deputy","derive","describe",
			"description","design","design","desire","desk","despite","destroy","detail",
			"detailed","determine","develop","development","device","die","difference",
			"different","difficult","difficulty","dinner","direct","direct","direction",
			"directly","director","disappear","discipline","discover","discuss","discussion",
			"disease","display","display","distance","distinction","distribution","district",
			"divide","division","do","doctor","document","dog","domestic","door","double",
			"doubt","down","down","draw","drawing","dream","dress","dress","drink",
			"drink","drive","drive","driver","drop","drug","dry","due","during","duty",
			"each","ear","early","early","earn","earth","easily","east","easy","eat",
			"economic","economy","edge","editor","education","educational","effect",
			"effective","effectively","effort","egg","either","either","elderly","election",
			"element","else","elsewhere","emerge","emphasis","employ","employee","employer",
			"employment","empty","enable","encourage","end","end","enemy","energy",
			"engine","engineering","enjoy","enough","enough","ensure","enter","enterprise",
			"entire","entirely","entitle","entry","environment","environmental","equal",
			"equally","equipment","error","escape","especially","essential","establish",
			"establishment","estate","estimate","even","evening","event","eventually",
			"ever","every","everybody","everyone","everything","evidence","exactly",
			"examination","examine","example","excellent","except","exchange","executive",
			"exercise","exercise","exhibition","exist","existence","existing","expect",
			"expectation","expenditure","expense","expensive","experience","experience",
			"experiment","expert","explain","explanation","explore","express","expression",
			"extend","extent","external","extra","extremely","eye","face","face","facility",
			"fact","factor","factory","fail","failure","fair","fairly","faith","fall",
			"fall","familiar","family","famous","far","far","farm","farmer","fashion",
			"fast","fast","father","favour","fear","fear","feature","fee","feel","feeling",
			"female","few","few","field","fight","figure","file","fill","film","final",
			"finally","finance","financial","find","finding","fine","finger","finish",
			"fire","firm","first","fish","fit","fix","flat","flight","floor","flow",
			"flower","fly","focus","follow","following","food","foot","football","for",
			"for","force","force","foreign","forest","forget","form","form","formal",
			"former","forward","foundation","free","freedom","frequently","fresh","friend",
			"from","front","front","fruit","fuel","full","fully","function","fund",
			"funny","further","future","future","gain","game","garden","gas","gate",
			"gather","general","general","generally","generate","generation","gentleman",
			"get","girl","give","glass","go","goal","god","gold","good","good","government",
			"grant","grant","great","green","grey","ground","group","grow","growing",
			"growth","guest","guide","gun","hair","half","half","hall","hand","hand",
			"handle","hang","happen","happy","hard","hard","hardly","hate","have","he",
			"head","head","health","hear","heart","heat","heavy","hell","help","help",
			"hence","her","her","here","herself","hide","high","high","highly","hill",
			"him","himself","his","his","historical","history","hit","hold","hole",
			"holiday","home","home","hope","hope","horse","hospital","hot","hotel",
			"hour","house","household","housing","how","however","huge","human","human",
			"hurt","husband","i","idea","identify","if","ignore","illustrate","image",
			"imagine","immediate","immediately","impact","implication","imply","importance",
			"important","impose","impossible","impression","improve","improvement",
			"in","in","incident","include","including","income","increase","increase",
			"increased","increasingly","indeed","independent","index","indicate","individual",
			"individual","industrial","industry","influence","influence","inform","information",
			"initial","initiative","injury","inside","inside","insist","instance","instead",
			"institute","institution","instruction","instrument","insurance","intend",
			"intention","interest","interested","interesting","internal","international",
			"interpretation","interview","into","introduce","introduction","investigate",
			"investigation","investment","invite","involve","iron","island","issue",
			"issue","it","item","its","itself","job","join","joint","journey","judge",
			"judge","jump","just","justice","keep","key","key","kid","kill","kind",
			"king","kitchen","knee","know","knowledge","labour","labour","lack","lady",
			"land","language","large","largely","last","last","late","late","later",
			"latter","laugh","launch","law","lawyer","lay","lead","lead","leader","leadership",
			"leading","leaf","league","lean","learn","least","leave","left","leg","legal",
			"legislation","length","less","less","let","letter","level","liability",
			"liberal","library","lie","life","lift","light","light","like","like","likely",
			"limit","limit","limited","line","link","link","lip","list","listen","literature",
			"little","little","little","live","living","loan","local","location","long",
			"long","look","look","lord","lose","loss","lot","love","love","lovely",
			"low","lunch","machine","magazine","main","mainly","maintain","major","majority",
			"make","male","male","man","manage","management","manager","manner","many",
			"map","mark","mark","market","market","marriage","married","marry","mass",
			"master","match","match","material","matter","matter","may","may","maybe",
			"me","meal","mean","meaning","means","meanwhile","measure","measure","mechanism",
			"media","medical","meet","meeting","member","membership","memory","mental",
			"mention","merely","message","metal","method","middle","might","mile","military",
			"milk","mind","mind","mine","minister","ministry","minute","miss","mistake",
			"model","modern","module","moment","money","month","more","more","morning",
			"most","most","mother","motion","motor","mountain","mouth","move","move",
			"movement","much","much","murder","museum","music","must","my","myself",
			"name","name","narrow","nation","national","natural","nature","near","nearly",
			"necessarily","necessary","neck","need","need","negotiation","neighbour",
			"neither","network","never","nevertheless","new","news","newspaper","next",
			"next","nice","night","no","no","no","no","nobody","nod","noise","none",
			"nor","normal","normally","north","northern","nose","not","note","note",
			"nothing","notice","notice","notion","now","nuclear","number","nurse","object",
			"objective","observation","observe","obtain","obvious","obviously","occasion",
			"occur","odd","of","off","off","offence","offer","offer","office","officer",
			"official","official","often","oil","okay","old","on","on","once","once",
			"one","only","only","onto","open","open","operate","operation","opinion",
			"opportunity","opposition","option","or","order","order","ordinary","organisation",
			"organise","organization","origin","original","other","other","other","otherwise",
			"ought","our","ourselves","out","outcome","output","outside","outside",
			"over","over","overall","own","own","owner","package","page","pain","paint",
			"painting","pair","panel","paper","parent","park","parliament","part","particular",
			"particularly","partly","partner","party","pass","passage","past","past",
			"past","path","patient","pattern","pay","pay","payment","peace","pension",
			"people","per","percent","perfect","perform","performance","perhaps","period",
			"permanent","person","personal","persuade","phase","phone","photograph",
			"physical","pick","picture","piece","place","place","plan","plan","planning",
			"plant","plastic","plate","play","play","player","please","pleasure","plenty",
			"plus","pocket","point","point","police","policy","political","politics",
			"pool","poor","popular","population","position","positive","possibility",
			"possible","possibly","post","potential","potential","pound","power","powerful",
			"practical","practice","prefer","prepare","presence","present","present",
			"present","president","press","press","pressure","pretty","prevent","previous",
			"previously","price","primary","prime","principle","priority","prison",
			"prisoner","private","probably","problem","procedure","process","produce",
			"product","production","professional","profit","program","programme","progress",
			"project","promise","promote","proper","properly","property","proportion",
			"propose","proposal","prospect","protect","protection","prove","provide",
			"provided","provision","pub","public","public","publication","publish",
			"pull","pupil","purpose","push","put","quality","quarter","question","question",
			"quick","quickly","quiet","quite","race","radio","railway","rain","raise",
			"range","rapidly","rare","rate","rather","reach","reaction","read","reader",
			"reading","ready","real","realise","reality","realize","really","reason",
			"reasonable","recall","receive","recent","recently","recognise","recognition",
			"recognize","recommend","record","record","recover","red","reduce","reduction",
			"refer","reference","reflect","reform","refuse","regard","region","regional",
			"regular","regulation","reject","relate","relation","relationship","relative",
			"relatively","release","release","relevant","relief","religion","religious",
			"rely","remain","remember","remind","remove","repeat","replace","reply",
			"report","report","represent","representation","representative","request",
			"require","requirement","research","resource","respect","respond","response",
			"responsibility","responsible","rest","rest","restaurant","result","result",
			"retain","return","return","reveal","revenue","review","revolution","rich",
			"ride","right","right","right","ring","ring","rise","rise","risk","river",
			"road","rock","role","roll","roof","room","round","round","route","row",
			"royal","rule","run","run","rural","safe","safety","sale","same","sample",
			"satisfy","save","say","scale","scene","scheme","school","science","scientific",
			"scientist","score","screen","sea","search","search","season","seat","second",
			"secondary","secretary","section","sector","secure","security","see","seek",
			"seem","select","selection","sell","send","senior","sense","sentence","separate",
			"separate","sequence","series","serious","seriously","servant","serve",
			"service","session","set","set","settle","settlement","several","severe",
			"sex","sexual","shake","shall","shape","share","share","she","sheet","ship",
			"shoe","shoot","shop","short","shot","should","shoulder","shout","show",
			"show","shut","side","sight","sign","sign","signal","significance","significant",
			"silence","similar","simple","simply","since","since","sing","single","sir",
			"sister","sit","site","situation","size","skill","skin","sky","sleep","slightly",
			"slip","slow","slowly","small","smile","smile","so","so","social","society",
			"soft","software","soil","soldier","solicitor","solution","some","somebody",
			"someone","something","sometimes","somewhat","somewhere","son","song","soon",
			"sorry","sort","sound","sound","source","south","southern","space","speak",
			"speaker","special","species","specific","speech","speed","spend","spirit",
			"sport","spot","spread","spring","staff","stage","stand","standard","standard",
			"star","star","start","start","state","state","statement","station","status",
			"stay","steal","step","step","stick","still","stock","stone","stop","store",
			"story","straight","strange","strategy","street","strength","strike","strike",
			"strong","strongly","structure","student","studio","study","study","stuff",
			"style","subject","substantial","succeed","success","successful","such",
			"suddenly","suffer","sufficient","suggest","suggestion","suitable","sum",
			"summer","sun","supply","supply","support","support","suppose","sure","surely",
			"surface","surprise","surround","survey","survive","switch","system","table",
			"take","talk","talk","tall","tape","target","task","tax","tea","teach",
			"teacher","teaching","team","tear","technical","technique","technology",
			"telephone","television","tell","temperature","tend","term","terms","terrible",
			"test","test","text","than","thank","thanks","that","that","the","theatre",
			"their","them","theme","themselves","then","theory","there","there","therefore",
			"these","they","thin","thing","think","this","those","though","though",
			"thought","threat","threaten","through","through","throughout","throw",
			"thus","ticket","time","tiny","title","to","to","to","today","together",
			"tomorrow","tone","tonight","too","tool","tooth","top","top","total","total",
			"totally","touch","touch","tour","towards","town","track","trade","tradition",
			"traditional","traffic","train","train","training","transfer","transfer",
			"transport","travel","treat","treatment","treaty","tree","trend","trial",
			"trip","troop","trouble","true","trust","truth","try","turn","turn","twice",
			"type","typical","unable","under","under","understand","understanding",
			"undertake","unemployment","unfortunately","union","unit","united","university",
			"unless","unlikely","until","until","up","up","upon","upper","urban","us",
			"use","use","used","used","useful","user","usual","usually","value","variation",
			"variety","various","vary","vast","vehicle","version","very","very","via",
			"victim","victory","video","view","village","violence","vision","visit",
			"visit","visitor","vital","voice","volume","vote","vote","wage","wait",
			"walk","walk","wall","want","war","warm","warn","wash","watch","water",
			"wave","way","we","weak","weapon","wear","weather","week","weekend","weight",
			"welcome","welfare","well","well","west","western","what","whatever","when",
			"when","where","where","whereas","whether","which","while","while","whilst",
			"white","who","whole","whole","whom","whose","why","wide","widely","wife",
			"wild","will","will","win","wind","window","wine","wing","winner","winter",
			"wish","with","withdraw","within","without","woman","wonder","wonderful",
			"wood","word","work","work","worker","working","works","world","worry",
			"worth","would","write","writer","writing","wrong","yard","yeah","year",
			"yes","yesterday","yet","you","young","your","yourself","youth"
	};
	/**
	 * Create a new empty instance of a user manager. The resulting user manager can
	 * be used to track user names and passwords, and handle permission checking.
	 * You must store at least
	 * one user name and password in the user manager for this to be useful.
	 * @param session The session that hosts this user managers
	 * @param defaultAutoSave the default setting for "auto-save" which automatically
	 * saves the user data when it is changed.
	 */
	public UserManager(JBasic session, boolean defaultAutoSave) {
		
		/*
		 * Create the map of users to user names, and instantiate a copy of the
		 * cipher mechanism for handling passwords.
		 */
		users = new HashMap<String,User>();
		crypto = new SimpleCipher(null);
				
		/*
		 * Other misc settings
		 */
		autoSave = defaultAutoSave;
		controllingSession = session;
		JBasic.userManager = this;
		dirtyData = false;
	}

	/**
	 * This is used to initialize the password hash function. It is here as
	 * a separate method so that it's possible for the user to create the
	 * SYS$PASSWORD_SALT variable to define a different password hash if
	 * desired after the UserManager and session are up and running but
	 * before the UserManager is first asked to process a password.
	 */
	public  synchronized void initializePasswordHash() {
		/*
		 * The "secret" is the seed used to create the hashes for passwords.
		 */
		if( secret == null ) {
			String passwordSalt = controllingSession.getString("SYS$PASSWORD_SALT");
			if( passwordSalt == null ) {
				passwordSalt = "sausages";
				//JBasic.log.info("Using default password encoding");
			}
			else {
				controllingSession.globals().deleteAlways("SYS$PASSWORD_SALT");
				JBasic.log.info("Using non-default password encoding");
			}
			secret = crypto.encryptedString("zrbtt", passwordSalt);
		}

	}
	/**
	 * Set the current controllingSession for the user manager.  
	 * @param currentSession The controllingSession that
	 * is activating multiuser mode.
	 */
	public void setSession( JBasic currentSession ) {
		controllingSession = currentSession;
	}

	/**
	 * Return the active controllingSession that started multiuser mode.
	 * @return a JBasic instance.
	 */
	public JBasic getSession() {
		return controllingSession;
	}

	/**
	 * Return the name of the database file where information is
	 * stored.
	 * @return string with the file name.
	 */
	public String getDatabaseFileName() {
		return databaseFileName;
	}
	/** 
	 * Add a new user to the user database.
	 * @param name the user name
	 * @param password then plain text password
	 * @return the newly created User object (which is also now registered in
	 * the user manager data)
	 */
	public User addUser( String name, String password ) {

		initializePasswordHash();
		if( crypto == null )
			crypto = new SimpleCipher(this.controllingSession);
		User user = new User( name.toUpperCase(), crypto, password );
		users.put(name, user);
		dirtyData = true;
		return user;
	}

	/**
	 * Set the password for an existing user.  It is an error if the username does
	 * not exist.  The password is passed as plain text and encrypted by this routine.
	 * @param userName the user name to get the new password
	 * @param password the new password value
	 * @return Status indicating if the encryption was successful
	 */
	public Status setPassword(String userName, String password) {
		
		/*
		 * Find the user info for the given user name. If there is
		 * no such user, complain.
		 */
		User user = users.get(userName.toUpperCase());
		if( user == null )
			return new Status(Status.NOUSER, userName);
		
		/*
		 * Set the user password, using the "secret" which is the seed
		 * for the password encryption.
		 */
		dirtyData = true;
		initializePasswordHash();
		user.setPassword(password, secret);
		
		/*
		 * If after that the user object doesn't have a password, then
		 * something went wrong with the encryption. Report the error.
		 */
		if( !user.hasPassword())
			return crypto.getStatus();
		
		/*
		 * If each change to the user data is supposed to trigger a save,
		 * let's handle that now.  We save using an empty file name which
		 * means "re-use the last filename you had" so we save the same
		 * file over and over.
		 */
		if( autoSave )
			try {
				save("");
			} catch (JBasicException e) {
				e.printStackTrace();
			}

			return new Status();
	}

	/**
	 * Determine if a user name is valid
	 * @param name The user name to check
	 * @return true if the given name is valid; that is, if there is a matching
	 * user database record for the given user.
	 */
	public boolean isUser( String name ) {
		User user = users.get(name.toUpperCase());
		return (user != null);
	}

	/**
	 * Determine if a given name/password combination is valid.
	 * @param name the user name
	 * @param password the plain test password
	 * @return true if the given name and password are valid.
	 */
	public boolean authentic( String name, String password ) {
		
		/*
		 * Get the matching user record for the given user name.  IF
		 * there is no such user, return false indicating to the caller
		 * that there was an authentication error.
		 */
		User user = users.get(name.toUpperCase());
		if( user == null )
			return false;
		
		/*
		 * Get the hashed password string from the user data. If there
		 * is no password, and we were also passed a null indicating
		 * no password, then this is a match.
		 */
		String userPW = user.getPassword();
		if( userPW != null && userPW.length() < 1 && password == null)
			return true;
		if( userPW == null && password == null )
			return true;
		
		
		/*
		 * If the user database password is non-null but we were given
		 * a null, then there a password was given when not needed.  This
		 * is also an authentication failure.
		 */
		if( userPW != null && password == null )
			return false;
		
		/*
		 * If the data returned in the hash starts with an asterisk, remove
		 * it. This indicates an unencrypted password.
		 */
		if( userPW != null && userPW.length() > 1 &&userPW.charAt(0) == '*')
			userPW = userPW.substring(1);

		/*
		 * Get the hash of the password string given, unless it has an
		 * asterisk indicating it is already encrypted.
		 */
		String pw = null;
		initializePasswordHash();
		if( password.length() > 1 && password.charAt(0) == '*')
			pw = password.substring(1);
		else 
			pw = crypto.encryptedString(password, secret);

		/*
		 * Assuming we still have valid password strings, compare the
		 * hash values and return the result.s
		 */
		if( userPW != null )
			return userPW.equals(pw);
		return false;
	}

	/**
	 * Return flag indicating if authentication is required
	 * @return true if there are one or more user names stored in the
	 * UserManager, requiring authentication. If there are no names in
	 * the user manager, then authentication is not required (or possible).
	 */
	public boolean requireAuthentication() {
		return (users.size() > 0);
	}

	/**
	 * Set a permission for a named user.
	 * @param userName the name of the user.
	 * @param permission a string containing the permission name
	 * @param state true if the permission is to be granted, else false
	 */
	public void setPermission( String userName, String permission, boolean state) {

		String theName = userName.toUpperCase();
		User user = users.get(theName);
		if( user != null ) {
			if( permission.equalsIgnoreCase("MASK") && state == true)
				user.setPermissionMask();
			else {
				user.setPermission(permission.toUpperCase(), state);
				dirtyData = true;
			}
		}
	}

	/**
	 * Check to see if a given user name has a given permission
	 * @param userName the user name to check
	 * @param permission the permission name
	 * @return true if the user has been granted the permission.
	 */
	public boolean hasPermission( String userName, String permission ) {
		if( userName == null )
			return false;
		User user = users.get(userName.toUpperCase());
		if( user == null )
			return false;
		return (user.hasPermission(permission.toUpperCase()));

	}

	/**
	 * Return an array of string names of the permissions for a given user.
	 * @param userName the name of the user to return permissions for.
	 * @return a Value containing an array of strings, or null if there was
	 * no such user.
	 */
	public Value permissions( String userName ) {
		User user = users.get(userName.toUpperCase());
		if( user == null )
			return null;

		return user.getPermissions();
		
	}
	/**
	 * Given a Value containing a record of USER data, create a new User
	 * object in the current record for that information.  If the record
	 * contains information that matches an already existing record, that
	 * record is replaced.
	 * @param userRecord The Value containing a RECORD with the required
	 * and optional user data.  At a minimum, there must be a USER and a
	 * PASSWORD field in the record.
	 * @param createUser true if the user record is created by this operation
	 * @return a Status indicating if the operation was successful.
	 */
	public Status loadUserRecord(Value userRecord, boolean createUser) {

		if( userRecord.getType() != Value.RECORD ) {
			return new Status(Status.ARGERR, "Value is not a RECORD");
		}

		Value userValue = userRecord.getElement("USER");
		if( userValue == null )
			return new Status(Status.ARGERR, "Value has no USER member");

		Value pwValue = userRecord.getElement("PASSWORD");
		if( createUser && (pwValue == null )) 
			return new Status(Status.ARGERR, "Value has no PASSWORD member");

		User user = null;
		String userName = userValue.getString().toUpperCase();
		initializePasswordHash();

		if( createUser ) {
			String pws = pwValue.getString();
			if( pws.equals("*"))
				pws = null;
			user = addUser(userName, pws);
		}
		else {
			user = users.get(userName);
			if( pwValue != null && user != null ) 
				user.setPassword( "*" + pwValue.getString().toUpperCase(), secret);
		}
		if( user == null ) {
			return new Status(Status.NOUSER, userName);
		}


		Value wsValue = userRecord.getElement("WORKSPACE");
		if( wsValue == null & createUser ) {
			wsValue = controllingSession.expression(
				" \"_WS::workspace-" + userValue.getString() + "-\" +" +
						"password(string(timecode())) + \".jbasic\"");
			
			//wsValue = new Value("_WS::workspace-" + userValue.getString() + ".jbasic");
		}
		if( wsValue != null )
			user.setWorkspace(wsValue.getString());

		Value hmValue = userRecord.getElement("HOME");
		if( hmValue == null & createUser ) {
			String sep = System.getProperty("file.separator");
			String baseName = System.getProperty("java.io.tmpdir");
			String def = baseName + sep + "jbasic" + sep + user.getName();
			hmValue = new Value( def );
		}
		if( hmValue != null ) {
			user.setHome(hmValue.getString());
		}
		Value acctValue = userRecord.getElement("ACCOUNT");
		if( acctValue == null & createUser )
			acctValue = new Value("DEFAULT");
		if( acctValue != null )
			user.setAccount(acctValue.getString());

		Value nameValue = userRecord.getElement("NAME");
		if( nameValue == null & createUser )
			nameValue = new Value("DEFAULT");
		if( nameValue != null )
			user.setFullName(nameValue.getString());

		Value loginCount = userRecord.getElement("COUNT");
		if( loginCount == null )
			loginCount = new Value(0);
		user.setLoginCount(loginCount.getInteger());
		
		Value permissions = userRecord.getElement("PERMISSIONS");
		if( permissions != null) {
			for( int ix = 1; ix <= permissions.size(); ix++ ) {
				user.setPermission(permissions.getString(ix), true);
			}
			user.setPermissionMask();
		}

		return new Status();
	}
	/**
	 * Load the user information from an array of records.
	 * 
	 * @param userList a value containing the user data.
	 * @throws JBasicException if the value is not an array
	 * of records or the records are incorrectly formed.
	 */
	public void load( Value userList ) throws JBasicException {

		/*
		 * If we are given a null parameter, use the default text file.
		 */
		if( userList == null ) {
			load(JBasic.USER_DATA);
			return;
		}
		/*
		 * If the user data is a string, it must be a file name containing
		 * the user data.
		 */
		if( userList.getType() == Value.STRING ) {
			load(userList.getString());
			return;
		}

		/*
		 * Otherwise it must be an array with the direct record data in the
		 * array.
		 */
		if( userList.getType() != Value.ARRAY) {
			throw new JBasicException(Status.ARGERR, "user list is not an ARRAY");
		}

		/*
		 * Scan over the array, and make sure each element is converted to
		 * a user.  Report any errors.
		 */
		for( int ix = 0; ix < userList.size(); ix++ ) {
			Value userRecord = userList.getElement(ix+1);
			Status sts = loadUserRecord(userRecord, true);
			if( sts.failed()) {
				Status m = new Status(Status.SERVER, "Invalid user database array element at position " 
						+ (ix+1));
				m.print(controllingSession);
				throw new JBasicException(sts);
			}
		}
		return;
	}


	/**
	 * Load the user data from an external file.  
	 * @param fileName the name of the file to load user data from
	 * @throws JBasicException an error occurred in parsing the XML
	 * data
	 */
	public void load( String fileName ) throws JBasicException {
		int count = 0;
		
		/*
		 * If we are given a filename, then use that for the file path
		 * for the database file.  If the filename was null or was an
		 * empty string, then use the default name.
		 */
		databaseFileName = fileName;
		if( fileName == null || fileName.length() == 0 )
			databaseFileName = JBasic.USER_DATA;

		/*
		 * Open the file, and get the full path name back from the
		 * file object.  This becomes the fully-qualified current
		 * database file name.
		 */
		File f = new File( databaseFileName);
		try {
			databaseFileName = f.getCanonicalPath();
		} catch (IOException e) {
			/* This should NEVER happen! */
			e.printStackTrace();
		}

		/*
		 * Create a JBasic input file and open it using the filename just
		 * created. The global symbol table is passed to give the file a
		 * place to store the file handle data.
		 */
		JBFInput file = new JBFInput(controllingSession);
		file.open(new Value(databaseFileName), controllingSession.globals());

		/*
		 * Create an XML manager wich will be used to read and parse the XML data.
		 */
		XMLManager xml = new XMLManager(controllingSession);
		
		/*
		 * Read the XML data from the file.  This recognizes tags and knows how
		 * far into the file to read to get a coherent and valid XML data structure.
		 */
		String line = null;
		try {
			line = xml.readXML(controllingSession, file, false);
		}
		catch (JBasicException e) {
			throw e;
		}
		
		/*
		 * Give the string to the XML manager as a data buffer to parse, and then
		 * ask it to find a Value object encoded in the buffer.
		 */
		xml.setString(line);
		Value item = xml.parseXML(dictionaryXMLTag);
		if( item == null )
			throw new JBasicException(xml.getStatus());

		/*
		 * It's got to be a RECORD data type to be valid.  If not, something was bogus
		 * in the file - complain.
		 */
		if( item.getType() != Value.RECORD)
			throw new JBasicException(Status.SERVER, "invalid record at line " + count );

		/*
		 * Find the array of logical name data in the record.  Step through each array
		 * element (which are themselves records) and get the name/path pairs.  Each of
		 * these is added to the JBasic session's logical name manager data.
		 */
		Value list = item.getElement("LOGICAL_NAMES");
		if( list != null ) {
			for( int ix = 1; ix <= list.size(); ix++ ) {
				Value logicalName = list.getElement(ix);
				String theName = logicalName.getElement("NAME").getString();
				String thePath = logicalName.getElement("PATH").getString();
				controllingSession.getNamespace().addLogicalName(theName, thePath);
				JBasic.log.info("Define logical name " + theName + " to " + thePath);
			}
		}
		
		/*
		 * Same for user names - there is an array of user objects encoded as records.
		 * Step through each one and get the value out, and store it in the user manager.
		 */
		list = item.getElement("USER_NAMES");
		if( list != null ) {
			for( int ix = 1; ix <= list.size(); ix++ ) {
				Value userName = list.getElement(ix);
				loadUserRecord( userName, true );
				JBasic.log.info("Loading data for user " + userName.getElement("USER").getString().toUpperCase());
				JBasic.log.debug("Data detail = " + userName.getString());
			}
			JBasic.log.info("Loaded " + list.size() + " user records");
		}

		/*
		 * We're good, let's get out of Dodge. Close the file so it's not left dangling, and
		 * tell the user what we did for him.
		 */
		
		file.close();
		JBasic.log.info("User database loaded from " + databaseFileName);
		return;
	}

	/**
	 * Save the current user list to a file, passed by name
	 * @param fileName the String containing the file name path to store the
	 * user data.
	 * @throws JBasicException  there is no user data or a file I/O error
	 * occurs writing out the XML data structure
	 */
	public void save(String fileName) throws JBasicException {

		/*
		 * If the user manager has never really been instantiated, or there are
		 * no user records, then we have no work to do.  Note that if you create
		 * logical names but no users, you cannot save this data!
		 */
		if (users == null)
			throw new JBasicException(Status.SERVER, "no user data");
		if (users.size() == 0)
			throw new JBasicException(Status.SERVER, "no user data");

		/*
		 * Figure out the filename that we're going to use to write the data. The
		 * user passes in a filename, which can be a null or empty string in which
		 * case we use the last file name.
		 */
		String theName = fileName;

		if( fileName == null || fileName.length() == 0 )
			theName = databaseFileName;
		else {
			File f = new File(theName);
			try {
				theName = f.getCanonicalPath();
			} catch (IOException e) {
				throw new JBasicException(Status.FAULT, "User manager file error, " + 
						e.getMessage());
			}
			databaseFileName = theName;
		}

		/*
		 * Create an output file an dopen it, using the full canonical name.  The
		 * global symbol table is used to store the open file handle information.
		 */
		JBFOutput file = new JBFOutput(controllingSession);
		file.open(new Value(theName), controllingSession.globals());
		
		/*
		 * Put an XML header and comment in the file.
		 */
		final Date rightNow = new Date();
		file.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		file.print("<!--Saved authorization data, created " + rightNow.toString());
		file.println("-->");

		/*
		 * Iterate over the logical names in the name space for the controlling
		 * session.  For each one, create a record of name and path and add it 
		 * to the logical names array.
		 */
		Iterator ix = null;
		ix = controllingSession.getNamespace().list.keySet().iterator();
		XMLManager xml = new XMLManager(this.controllingSession);

		Value logicalNames = new Value(Value.ARRAY, null);

		while( ix.hasNext()) {
			String logicalName = (String) ix.next();
			String physicalName = controllingSession.getNamespace().getPhysicalPath(logicalName);
			Value lName = new Value(Value.RECORD,null);
			lName.setElement(new Value(logicalName), "NAME");
			lName.setElement(new Value(physicalName), "PATH");
			logicalNames.addElement(lName);
		}

		/*
		 * Create a second array that contains the list of user data.  Iterate over
		 * each user in the user manager, and capture their information in a record
		 * added to the userNames array.
		 */
		ix = users.keySet().iterator();
		Value userNames = new Value(Value.ARRAY, null);

		while (ix.hasNext()) {
			String un = (String) ix.next();
			User user = users.get(un);
			Value userRecord = new Value(Value.RECORD, null);
			userRecord.setElement(un, "USER");
			userRecord.setElement(user.isActive(),
			"ACTIVE");
			userRecord.setElement(user.getHome(), "HOME");
			userRecord.setElement(user.getWorkspace(), "WORKSPACE");
			userRecord.setElement(user.getPassword(), "PASSWORD");
			userRecord.setElement(user.getAccount(), "ACCOUNT");
			userRecord.setElement(user.getFullName(), "NAME");
			userRecord.setElement(permissions(un), "PERMISSIONS");
			userRecord.setElement(user.getLoginCount(), "LOGINCOUNT");
			userNames.addElement(userRecord);
		}

		/*
		 * Finally, create a master record that contains both the logical name
		 * and user name arrays.  Convert this to XML and write the string to 
		 * the output file.
		 */
		Value dict = new Value(Value.RECORD, null);
		dict.setElement(logicalNames, "LOGICAL_NAMES");
		dict.setElement(userNames, "USER_NAMES");
		file.println(xml.toXML(dictionaryXMLTag, dict));
		
		/*
		 * All done. Close the file and also indicate that there is no unsaved
		 * user data at the moment.  Tell the user where we wrote the data and
		 * get out.
		 */
		file.close();
		dirtyData = false;
		JBasic.log.info("User database written to " + databaseFileName);

		return;
	}
	
	/**
	 * Mark a user session in the active user database as active or
	 * inactive.
	 * 
	 * @param theUser the name of the user who'se state is being changed
	 * @param session the JBasic session of the user login (not the controlling
	 * session but the one created for the individual user login)
	 * @param isActive true if the user session is to be made active, false
	 * if it is going inactive.
	 * @return true if the session was able to be activated
	 */
	public boolean active(final  String theUser, JBasic session, boolean isActive) {
		
		/*
		 * Fix the username to uppercase which is how all names are normalized
		 * in the user manager.
		 */
		if( theUser == null )
			return false;
		User user = users.get(theUser.toUpperCase());

		/*
		 * This is synchronized so that racing threads for multiple logins or
		 * logouts by the same user cannot collide.
		 */
		synchronized (user) {
			
			/*
			 * If the map of active sessions has never been created, now is the
			 * time to do it.  This is a map keyed by session ID string that
			 * contains a reference to the actual JBasic object hosting the
			 * user's login session. 
			 */
			if( JBasic.activeSessions == null )
				JBasic.activeSessions = new TreeMap<String,JBasic>();
			
			String id;
			id = session.getInstanceID().toUpperCase();
			
			if( isActive ) {
				/*
				 * Bind the user to the session.  Grant the session access
				 * to the common name space.  And add this session to the
				 * list of active sessions.
				 */
				session.setUserIdentity(user);
				session.setNameSpace(controllingSession.getNamespace());
				JBasic.activeSessions.put(id, session);
				user.setLogin();
				dirtyData = true;
			}
			else {
				/*
				 * Remove the session from the list of sessions.  Assuming
				 * there was one, also zap it's user identity.
				 */
				Object o = JBasic.activeSessions.remove(id);
				if( o == null )
					return false;
				session.setUserIdentity(null);
			}
			return true;
		}
	}

	/**
	 * Return a Value that contains an array of records with the names and
	 * other information for all users.
	 * @return the list of users, or an empty list if there are no users.
	 */
	public Value userList() {
		
		/*
		 * Data is returned as a JBasic array of records. Create the array.
		 * If there are no users, then we have done all we can and the
		 * empty array should go back.
		 */
		Value userList = new Value(Value.ARRAY, null);
		if (users == null)
			return userList;
		if (users.size() == 0)
			return userList;

		/*
		 * Iterate over each user by name.  For each user, create a
		 * record that contains descriptive information about the
		 * user.  This record is added to the user list array.
		 */
		Iterator ix = users.keySet().iterator();
		while (ix.hasNext()) {
			String un = (String) ix.next();
			User user = users.get(un);
			Value userRecord = new Value(Value.RECORD, null);
			userRecord.setElement(un, "USER");
			userRecord.setElement(user.isActive(), "ACTIVE");
			userRecord.setElement(user.getHome(), "HOME");
			userRecord.setElement(user.getWorkspace(), "WORKSPACE");
			userRecord.setElement(user.getFullName(), "NAME");
			userRecord.setElement(user.getAccount(), "ACCOUNT");
			userRecord.setElement(user.getLoginCount(), "LOGINCOUNT");
			userList.addElement(userRecord);
		}
		return userList;
	}

	/**
	 * Get the workspace name for a given user.
	 * 
	 * @param theUser the name of the user to look up
	 * @return name of the workspace for this user.
	 */
	public String getWorkspace(String theUser) {

		final String defaultWorkspaceName = "workspace-default.jbasic";
		/*
		 * If there is no functioning user manager yet, return the default
		 * workspace name.
		 */
		if( users == null || users.size() == 0 )
			return defaultWorkspaceName;

		/*
		 * If the user name given doesn't yet have a matching user data
		 * base entry, return the default name.
		 */
		User user = users.get(theUser.toUpperCase());
		if( user == null )
			return defaultWorkspaceName;

		/*
		 * Otherwise, return the workspace name stored for the user in the
		 * database.
		 */
		return user.getWorkspace();
	}

	/**
	 * Get the home directory default for this user.
	 * @param userName a string containing the specific user home directory
	 * or a default string path created from the user name.
	 * @return the path string with no trailing separator
	 */
	public String home(String userName) {

		/*
		 * Construct some default strings used to construct the default names
		 * and locations for home directories.
		 */
		String sep = System.getProperty("file.separator");
		String baseName = System.getProperty("java.io.tmpdir");
		String def = baseName + sep + "jbasic" + sep + "default";

		/*
		 * If there is no user manager yet (very unlikely) then return
		 * the default name which is stored in the local file system's
		 * temporary directory space.
		 */
		if( users == null || users.size() == 0 )
			return def;

		/*
		 * If the user doesn't exist yet, then also return the default
		 * home directory.
		 */
		User user = users.get(userName.toUpperCase());
		if( user == null ) 
			return def;

		/*
		 * If no one has set up a name for this user yet, create a useful
		 * default.
		 */
		if( user.getHome() == null )
			user.setHome( baseName + sep + "jbasic" + sep + userName );

		return user.getHome();

	}

	/**
	 * Delete a username from the active user manager.  The record is removed
	 * from the runtime version of the user data; a separate operation must
	 * be performed if this change is to be persisted to disk.
	 * @param userName the user name to delete.
	 * @return Status indicating if the name was successfully deleted
	 */
	public Status deleteUser(String userName) {

		/*
		 * If the user manager isn't set up yet, then there can be no delete
		 * for this user.  Also, search for the user in the user manager data
		 * and see if s/he exists.  IF not, there can be no delete.
		 */
		if( users == null )
			return new Status(Status.NOUSER, userName);
		String un = userName.toUpperCase();
		User user = users.get(un);
		if( user == null )
			return new Status(Status.NOUSER, un);

		/*
		 * Remove the user data from the map, and indicate that the user data
		 * has changed.
		 */
		users.remove(un);
		dirtyData = true;

		return new Status();
	}

	/**
	 * @return number of user names in the user manager.
	 */
	public int size() {
		if( users == null )
			return 0;
		return users.size();
	}

	/**
	 * For a given user name, determine if they are currently logged in.
	 * Scan the list of active sessions to see if one of them matches
	 * the named user object.
	 * @param userName the username to check
	 * @return true if there is already an active session for this user.
	 */
	public boolean isActive(String userName) {
		
		/*
		 * If the user manager isn't set up yet, then the user cannot
		 * be active. If there are no active sessions, the user cannot
		 * be active. Otherwise, look up the user in the database.
		 */
		if( users == null )
			return false;
		if( JBasic.activeSessions == null )
			return false;
		User user = users.get(userName.toUpperCase());
		
		/*
		 * Search the list of active sessions to see if any of them are
		 * owned by the given user.  If there is a session owned by
		 * the user, then return true. Otherwise, the user is not active.
		 */
		Iterator i = JBasic.activeSessions.keySet().iterator();
		while( i.hasNext()) {
			String key = (String) i.next();
			JBasic s = JBasic.activeSessions.get(key);
			if( s.getUserIdentity() == user )
				return true;
		}
		return false;
	}

	/**
	 * Take a path specification and modify it in whatever ways are required
	 * by the current user's settings to be a true file system path. If
	 * the user is not in sandbox mode or has FSNAMES permissions, then no
	 * file name conversion is done.
	 * @param session the current JBasic session
	 * @param path the user-mode path specification
	 * @return a String expressing the native file system path.
	 * @throws JBasicException The user does not have permission to 
	 * formulate file system native paths.
	 */
	public String makeFSPath( JBasic session, String path ) throws JBasicException {

		/*
		 * If the active session doesn't yet exist then there is no path - return 
		 * a null.  If we are not in sandbox mode, then there are no changes to be
		 * made to the path.
		 */
		if( session == null )
			return path;
		
		if( !session.inSandbox())
			return path;

		/*
		 * Get the user identity for the current session.  IF the user has the
		 * FS_NAMES permission,  then the path name is left as-is becasue the
		 * user is allowed to directly access file system objects.
		 */
		User user = session.getUserIdentity();
		if( user.hasPermission(Permissions.FS_NAMES))
			return path;

		/*
		 * We cannot allow ".." syntax that references relative positions
		 * above the current location.  First clean up duplicate "/" characters
		 * which are meaningless.  Then check to see if the "parent" link would
		 * ever rise out of the self-imposed top-level of the file system we
		 * are allowed to see.
		 */
		
		StringBuffer tempPath = new StringBuffer(path);
		
		String sep = System.getProperty("file.separator");
		String dualSep = sep + sep;
		
		int n = tempPath.indexOf(dualSep);
		while( n > -1 ) {
			tempPath.deleteCharAt(n);
			n = tempPath.indexOf(dualSep);
		}

		path = tempPath.toString();

		int depth = 0;
		n = 0;
		if( tempPath.indexOf(sep) == 0)
			n++;
		
		int len = tempPath.length();
		if( tempPath.substring(len-1).equals(sep))
			len--;
		
		for( ; n < len; n++ ) {
			if( tempPath.substring(n).startsWith(sep)) {
				if( !tempPath.substring(n+1).startsWith(".."))
					depth++;
			}
			else
				if( tempPath.substring(n).startsWith("..")) {
					depth--;
					if( depth < 0)
						throw new JBasicException(Status.INVPATH, tempPath.toString());
					n++;
				}
			
		}
		
		// JBasic.log.debug("Relative FSPATH=\"" + tempPath + "\", depth=" + depth);
		
		/*
		 * Convert any logical names.  If there are none (i.e. logical and
		 * physical paths are identical) then we are done.
		 */
		String fsPath = session.getNamespace().logicalToPhysical(path);
		if( !fsPath.equals(path))
			return fsPath;

		/*
		 * If the path is already normalized to the user's home directory
		 * location then we have no work to do.
		 */
		if( path.startsWith( user.getHome()))
			return path;

		/*
		 * Create a buffer that will hold the user's home prefix, an optional
		 * separator if not already part of the provided path, and then the
		 * provided path.
		 */
		StringBuffer result = new StringBuffer(user.getHome());
		if( !path.startsWith(sep))
			result.append(sep);
		result.append(path);

		return result.toString();
	}

	/**
	 * Take a path specification and modify it in whatever ways are required
	 * by the current user's settings to be a user-specific path string.  If
	 * the user is not in sandbox mode or has FSNAMES permissions, then no
	 * file name munging is done.
	 * @param session the current JBasic session
	 * @param path the user-mode path specification
	 * @return a String expressing the native file system path.
	 */

	public String makeUserPath( JBasic session, String path ) {
		
		/*
		 * If no session, not in a sandbox, or no active user then don't
		 * do anything to the path string.
		 */
		
		if( session == null )
			return path;
		if(!session.inSandbox())
			return path;
		if( session.getUserIdentity() == null )
			return path;

		/*
		 * If teh current user already has FS_NAMES permission then we
		 * do no more work - the user is allowed to access file system
		 * objects directly.
		 */
		User user = session.getUserIdentity();
		if( user.hasPermission(Permissions.FS_NAMES))
			return path;

		/*
		 * If the path starts with the assigned home directory for the
		 * login user, then remove that assigned (fake) prefix and put the
		 * real one back in it's place.
		 */
		String sep = System.getProperty("file.separator");
		if( path.startsWith(user.getHome())) {
			int prefixLength = user.getHome().length();
			StringBuffer result = new StringBuffer(sep);
			result.append(path.substring(prefixLength));
			return result.toString();
		}
		
		/*
		 * Might be a logical name path.  Return it translated if
		 * possible, else the user's string as given.
		 */
		return session.getNamespace().physicalToLogical(path);
	}

	/**
	 * Mark the login time stamp for the current user.
	 * @param userName the user name
	 * @param session  the session the user logged in to
	 */
	public void recordLogin(String userName, JBasic session) {
		User user = users.get(userName.toUpperCase());
		user.setLogin();
		dirtyData = true;
	}

	/**
	 * Return a string indicating the last time a user logged in.
	 * @param userName the user name
	 * @return string with the time stamp
	 */
	public String lastLogin(String userName) {
		User user = users.get(userName.toUpperCase());
		if( user == null )
			return "NO SUCH USER";

		int count = user.getLoginCount();
		return user.getLogin() + ((count>0) ? ";" + count : "");

	}

	/**
	 * Let the caller know if the user data has been updated in some
	 * way that would justify saving new information.
	 * @return true if the user database has been modified since the last
	 * save operation.
	 */
	public boolean dirtyData() {
		return dirtyData;
	}

	/**
	 * Handle directory setup for server mode. This creates any user directories
	 * or logical name locations that do not already exist.
	 * @param adminPassword New password to set on ADMIN account (used to recover
	 * a system with damaged data, or null if no new password setting required.
	 * @throws JBasicException The path for the user data is invalid or the
	 * XML of the user database is invalid.
	 */
	public void setup(String adminPassword) throws JBasicException {

		if( JBasic.userManager == null ) {
			throw new JBasicException(Status.FAULT, "No active user manager to set up");
		}
		if( JBasic.userManager.controllingSession == null ) {
			throw new JBasicException(Status.FAULT, "No active controlling session to set up");
		}
		
		/*
		 * If there are no defined users, create an ADMIN and GUEST account.
		 */
		
		String homePath = System.getProperty("java.io.tmpdir") +
			      System.getProperty("file.separator") +
			      "jbasic" +
			      System.getProperty("file.separator");
		initializePasswordHash();		
		String gpw=null;
		
		if( users.keySet().size() == 0 ) {
			JBasic.log.printMessage("SERVERINITACCOUNTS");
			JBasicFile inputFile = getSession().stdin;
			boolean generatedPassword = false;
			if( adminPassword == null) {
				String prompt = getSession().getMessage("SERVERPWPROMPT", "ADMIN");
				getSession().stdout.print(prompt);
				gpw = inputFile.read();
				if( gpw == null || gpw.trim().length() == 0 ) {
					generatedPassword = true;
					gpw = generatePassword(3);
				}
			}
			User u = new User("ADMIN", crypto, "secret");
			u.setHome( homePath + "admin");
			u.setAccount("USERS");
			u.setFullName("Administrator");
			u.setPassword(adminPassword == null ? gpw : adminPassword, UserManager.secret);
			if( generatedPassword)
				JBasic.log.printMessage("SERVERGENPW", gpw);
			
			Value wsValue = controllingSession.expression(
					" \"_WS::workspace-admin" + "-\" +" +
							"password(string(timecode())) + \".jbasic\"");
			u.setWorkspace(/* "Workspace.jbasic" */ wsValue.getString());

			users.put("ADMIN", u);
			
			u = new User("GUEST", crypto, "guest");
			u.setHome( homePath + "guest");
			u.setAccount("USERS");
			u.setFullName("Guest User");
			u.setPassword( null, UserManager.secret);
			wsValue = controllingSession.expression(
					" \"_WS::workspace-guest" + "-\" +" +
							"password(string(timecode())) + \".jbasic\"");
			u.setWorkspace(/* "Workspace.jbasic" */ wsValue.getString());
			
			users.put("GUEST", u);
	
			setPermission("admin", "ALL", true);
			setPermission("guest", "ALL", false);
			setPermission("guest", "DIR_IO", true);
			setPermission("guest", "FILE_IO", true);
		}
		
		if( adminPassword != null ) {
			User u = users.get("ADMIN");
			if( u == null ) {
				JBasic.log.printMessage("SERVERADDACCOUNT", "ADMIN");
				u = new User("ADMIN", crypto, "secret");
				u.setHome( homePath + "admin");
				u.setAccount("USERS");
				u.setFullName("Administrator");
				u.setPassword(adminPassword, UserManager.secret);
				u.setWorkspace("Workspace.jbasic");
				users.put("ADMIN", u);
			} else {
				JBasic.log.printMessage("SERVERPWRESET", "ADMIN");
				u.setPassword(adminPassword, UserManager.secret);
			}
		}
		/*
		 * Make sure all the user directories are created.
		 */
		Iterator i = users.keySet().iterator();
		JBasic.log.printMessage("SERVERVALIDHOME");
		
		while( i.hasNext()) {
			
			/*
			 * Get the next user's home directory path.
			 */
			String path = users.get(i.next()).getHome();
			int mode = 0;
			
			/*
			 * Try to create the path. Remember if we were able to create
			 * it versus it already existing so the log message is right.
			 */
			try {
				if( MkdirFunction.makeDir(path, true))
					mode = 2;
				else
					mode = 1;
			} catch (Exception e ) {
				JBasic.log.error(getSession().getMessage("SERVER", e.toString()));
				JBasic.log.error(getSession().getMessage("SERVERPATHERR", path));
			}
			if( mode == 1 )
				JBasic.log.info(getSession().getMessage("SERVERVALIDPATH", path));
			else
				JBasic.log.info(getSession().getMessage("SERVERCREATEPATH", path ));
			
		}
		
		/*
		 * Now make sure all the logical name locations also exist.
		 */
		
		i = JBasic.userManager.controllingSession.getNamespace().list.keySet().iterator();
		JBasic.log.printMessage("SERVERVALIDLOGICALS");
		while( i.hasNext()) {
			String logicalName = (String) i.next();
			String path = JBasic.userManager.controllingSession.getNamespace().list.get(logicalName);
			int mode = 0;
			try {
				if( MkdirFunction.makeDir(path, true))
					mode = 2;
				else
					mode = 1;
			} catch (JBasicException e ) {
				JBasic.log.error(getSession().getMessage("SERVER", e.toString()));
				JBasic.log.error(getSession().getMessage("SERVERPATHERR", path));
			}
			if( mode == 1 )
				JBasic.log.info(getSession().getMessage("SERVERVALIDPATH", path));
			else
				JBasic.log.info(getSession().getMessage("SERVERCREATEPATH", path ));
		}
	}

	/**
	 * For a given user ID, is there a password set?
	 * @param un the username to test.
	 * @return true if the username has a password, or if the username is
	 * not valid.  This is to prevent probing passwords by users.
	 */
	public boolean hasPassword(String un) {
		User u = users.get(un.toUpperCase());
		if( u == null )
			return true;
		String pw = u.getPassword();
		if( pw == null)
			return false;
		if( pw.length() == 0)
			return false;
		return true;		
	}
	
	/**
	 * Generate a random password string
	 * @param wordCount number of words to appear in password
	 * @return String value containing the password
	 */
	public String generatePassword(int wordCount) {
		if( wordCount == 0 )
			wordCount = 3;

		int maxWord = wordlist.length;

		StringBuffer result = new StringBuffer();
		for( int wc = 0; wc < wordCount; wc++ ) {

			int k1 = (int) (Math.random()*maxWord);
			if( k1 >= maxWord )
				k1 = maxWord;

			String w = wordlist[k1];
			for( int wpos = 0; wpos < w.length(); wpos++) {
				char nextChar = w.charAt(wpos);
				boolean upCase = Math.random() > PW_UPCASE_FREQ;
				if( nextChar == 'o' || nextChar == 'q')
					upCase = false;
				if( nextChar == 'l')
					upCase = true;

				if( upCase )
					nextChar = Character.toUpperCase(nextChar);
				result.append(nextChar);
				if( (int)(Math.random() * 10) > PW_HYPH_FREQ)
					result.append("-");

			}
		}

		int k3 = (int) (Math.random()*10000);
		result.append(Integer.toString(k3));

		return result.toString();

	}
}
