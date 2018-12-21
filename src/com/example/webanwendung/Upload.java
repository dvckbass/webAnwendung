package com.example.webanwendung;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.opensymphony.xwork2.ActionSupport;

public class Upload extends ActionSupport {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public static final String BLACKLIST_FILE_PATH = "E:\\DevTools\\Eclipse\\workspace\\WebAnwendung\\blacklist.txt";
	public static final String WHITELIST_FILE_PATH = "E:\\DevTools\\Eclipse\\workspace\\WebAnwendung\\whitelist.txt";
	
	
	private File file;
	private String contentType;
	private String filename;

	private String blacklist, blacklistProp, whitelist;
	private String name;
	private float minTag, maxTag;
	private float smallestTag = 1;
	private float largestTag = 7;

	private TreeMap<String, Integer> csv = new TreeMap<String, Integer>();
	private Map<String, List<String>> termSites = new HashMap<String, List<String>>();

	private List<String> blacklistTermsList = new ArrayList<String>();
	private List<String> blacklistSitesList = new ArrayList<String>();
	private List<String> whitelistList = new ArrayList<String>();

	File blacklistFile = new File(BLACKLIST_FILE_PATH);
	File whitelistFile = new File(WHITELIST_FILE_PATH);
	
	

	// We want to use a maximum font size of 38px for a tag,
	// so we set the maximum weight value to 38.0 for convenience.

	public Upload() throws IOException {
		if(blacklistFile.length() > 0) {
			blacklistTermsList = readList(blacklistFile, blacklistTermsList);
		}
		if(whitelistFile.length() > 0) {
			whitelistList = readList(whitelistFile, whitelistList);
		}
	}

	public void validate() {

		if (getName() == null || getName().length() == 0) {
			addFieldError("name", "Name is required");
		}
		if (getUpload() == null || getUpload().length() == 0) {
			addFieldError("upload", "File is required");
		}

	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public File getUpload() {
		return file;
	}

	public String getUploadContentType() {
		return contentType;
	}

	public String getUploadFileName() {
		return filename;
	}

	public void setUpload(File file) {
		this.file = file;
	}

	public void setUploadContentType(String contentType) {
		this.contentType = contentType;
	}

	public void setUploadFileName(String filename) {
		this.filename = filename;
	}

	public Map<String, Integer> getCsv() {
		return csv;
	}

	public float getMinTag() {
		return minTag;
	}

	public float getMaxTag() {
		return maxTag;
	}

	public float getSmallestTag() {
		return smallestTag;
	}

	public float getLargestTag() {
		return largestTag;
	}

	public void setBlacklist(String blacklist) {
		this.blacklist = blacklist;
	}

	public String getBlacklist() {
		return blacklist;
	}

	public String getWhitelist() {
		return whitelist;
	}

	public void setWhitelist(String whitelist) {
		this.whitelist = whitelist;
	}
	
	public void setBlacklistTermsList(List<String> blacklistTermsList) {
		this.blacklistTermsList = blacklistTermsList;
	}
	
	public List<String> getBlacklistTermsList() {
		return blacklistTermsList;
	}
	
	public void setWhitelistList(List<String> whitelistList) {
		this.whitelistList = whitelistList;
	}
	
	public List<String> getWhitelistList() {
		return whitelistList;
	}

	public String execute() throws FileNotFoundException, IOException {
		System.out.println("Execute");
		
		
		BufferedWriter blacklistWriter = new BufferedWriter(new FileWriter(blacklistFile));
		BufferedWriter whitelistWriter = new BufferedWriter(new FileWriter(whitelistFile));
		
		loadProp();
		System.out.println("property loaded");


		blacklistTermsList = splitList(blacklistTermsList, blacklist);
		whitelistList = splitList(whitelistList, whitelist);
		
		
		blacklistTermsList = readList(blacklistFile, blacklistTermsList);
		System.out.println("readlist called");
		
		
		readCSV();
		
		System.out.println("csv read");
		String key = csv.firstKey();
		System.out.println("first key obtained");
		Integer value = csv.get(key);
		minTag = value;
		maxTag = value;
		MinMaxTag();
		// splitBlacklist();
		// removeBlacklist();
		System.out.println("blacklistprop: " + blacklistProp);
		// splitBlacklist();

		System.out.println("splitList called");
		switch (blacklistProp) {
		case "words":
			if (blacklist.length() > 0) {
				System.out.println("blacklist size: " + blacklistTermsList.size());
				writeBlacklist(blacklistWriter);
				System.out.println("writeBlacklist called");
				removeBlacklist();
				System.out.println("removeBlacklist called");
			}
			if (whitelist.length() > 0) {
				System.out.println("whitelist size: " + whitelistList.size());
				writeWhitelist(whitelistWriter);
				// writeBlacklist(blacklistWriter);
				// removeBlacklist();
				System.out.println("writeWhitelist called");
			}

			break;
		case "sites":
			writeBlacklist(blacklistWriter);
			blacklistSitesList = putSitesToBlacklist();
			System.out.println("Sites to blacklist done");
			for(String bl: blacklistSitesList) {
				System.out.println("blacklistSitesList: " + bl);
			}
			getTermsFromSites();
			System.out.println("Terms obtained from sites");
			removeBlacklist();
			System.out.println("removeBlacklist called");
			break;
		}
		
		System.out.println("Name: " + name);
		return SUCCESS;
	}

	// csv Datei einlesen und Terms und Seiten in Hashmap speichern
	public void readCSV() throws FileNotFoundException, IOException {
		File file = getUpload();
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			String line, term, site;
			int count;
			String[] csvLists;
			String[] sitesArray;
			while ((line = br.readLine()) != null) {
				List<String> sites = new ArrayList<>();

				csvLists = line.split(";");
				term = csvLists[0];
				count = Integer.parseInt(csvLists[1]);
				site = csvLists[2];
				sitesArray = site.split(" ");
				if (whitelist.length() > 0) {
					System.out.println("whitelist found");
					for (String wl : whitelistList) {
						System.out.println("whitelist: " + wl);
						if (wl.equals(term)) {
							System.out.println("whitelist match terms: " + term);
							csv.put(term, count);
						}
					}
				} else {
					for (String s : sitesArray) {
						sites.add(s);
					}
					if (count > 5) {
						csv.put(term, count);
						termSites.put(term, sites);
					}
				}
			}
		}
	}

	public void MinMaxTag() {
		for (Map.Entry<String, Integer> entry : csv.entrySet()) {
			Integer value = entry.getValue();
			if (value < minTag && value > 0) {
				minTag = value;
			}
			if (value > maxTag) {
				maxTag = value;
			}
		}
	}

	/*
	 * public void splitBlacklist() {
	 * 
	 * blacklistTermsList = Arrays.asList(blacklist.split(";")); }
	 * 
	 * public void splitWhitelist() {
	 * 
	 * whitelistList = Arrays.asList(whitelist.split(";")); }
	 */

	public List<String> splitList(List<String> list, String listString) {
		list = new ArrayList(Arrays.asList(listString.split(";")));
		return list;
	}

	private void removeBlacklist() {
		for (String bl : blacklistTermsList) {
			if (csv.containsKey(bl)) {
				csv.remove(bl);
				System.out.println(bl + " removed");
			}
		}
	}

	private void loadProp() {
		Properties prop = new Properties();
		InputStream input = null;

		try {

			input = new FileInputStream("E:\\DevTools\\Eclipse\\workspace\\WebAnwendung\\config.properties");

			// load a properties file
			prop.load(input);

			// get the property value and print it out
			blacklistProp = prop.getProperty("blacklist");

		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

	public static Object getKeyFromValue(Map hm, Object value) {
		for (Object o : hm.keySet()) {
			if (hm.get(o).equals(value)) {
				return o;
			}
		}
		return null;
	}

	private List<String> putSitesToBlacklist() {
		String key;
		List<String> sitesList = new ArrayList<>();
		for (String term : blacklistTermsList) {
			for (String termMap : termSites.keySet()) {
				if (term.equals(termMap)) {
					// System.out.println("Term found: " + term);
					sitesList = termSites.get(termMap);
					/*
					 * for(String site: sitesList) { System.out.println("Site: " + site); }
					 */
				}
			}
		}
		sitesList = removeDuplicates(sitesList);
		return sitesList;
	}

	private void getTermsFromSites() {
		String term;
		
		// loop through blacklistsiteslist
		for (String blacklist : blacklistSitesList) {
			// loop through hashmap termsites to match value
			for (List<String> sites : termSites.values()) {
				// loop through sites to get each site
				for (String site : sites) {
					if (blacklist.equals(site)) {
						term = (String) getKeyFromValue(termSites, sites);
						System.out.println("blacklisttermslist size:" + blacklistTermsList.size());
						blacklistTermsList.add(term);
					}
				}
			}
		}
		blacklistTermsList = removeDuplicates(blacklistTermsList);
	}

	// remove duplicates from a list
	private List<String> removeDuplicates(List<String> list) {
		Set<String> duplicates = new HashSet<>();
		duplicates.addAll(list);
		list.clear();
		list.addAll(duplicates);
		return list;
	}

	// write blacklist to blacklist.txt
	private void writeBlacklist(BufferedWriter writer) throws IOException {
		System.out.println("writeblacklist called");
		for (String bl : blacklistTermsList) {
			System.out.println("blacklist: " + bl);
			if (csv.containsKey(bl)) {
				writer.write(bl + ";");
			}
		}
		writer.close();
	}

	private void writeWhitelist(BufferedWriter writer) throws IOException {
		System.out.println("writewhitelist called");
		for (String wh : whitelistList) {
			System.out.println("whitelist: " + wh);
			if (csv.containsKey(wh)) {
				writer.write(wh + ";");
			}
		}
		writer.close();
	}

	private List<String> readList(File file, List<String> list) throws IOException {
		List<String> lineString = new ArrayList<>();
		BufferedReader br = new BufferedReader(new FileReader(file));
		String line;
		
		while ((line = br.readLine()) != null) {
			lineString.add(line);
		}
		
		for (String str : lineString) {
			list.addAll(new ArrayList(Arrays.asList(str.split(";"))));
		}
	
		
		br.close();
		return list;
	}
}
