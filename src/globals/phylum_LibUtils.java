package globals;

import java.awt.Frame;
import java.awt.Point;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.prefs.Preferences;

import primitives.GraphicPrimitive;
import primitives.MacroDesc;

import primitives.GraphicPrimitive;
import primitives.MacroDesc;

/** Class to handle library files and databases.

<pre>
	This file is part of FidoCadJ.

    FidoCadJ is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    FidoCadJ is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with FidoCadJ.  If not, see <http://www.gnu.org/licenses/>.

	Copyright 2012-2013 by phylum2, Davide Bucci
</pre>

@author phylum2
*/


public class phylum_LibUtils {

	/** Extract all the macros belonging to a given library
		
		@param m the macro list
		@param libfile the file name of the wanted library
		
		@return the library.
	
	*/
	public static Map<String,MacroDesc> getLibrary(Map<String,MacroDesc> m,
		String libfile)
	{
		Map<String,MacroDesc> mm = new TreeMap<String,MacroDesc>();
		MacroDesc md;		
		for (Entry<String, MacroDesc> e : m.entrySet())
		{			
			md = e.getValue();
			
			// The most reliable way to discriminate the macros is to watch
			// at the prefix in the key, i.e. everything which comes 
			// before the dot in the complete key.
			
			int dotPos = md.key.lastIndexOf(".");
			
			// If no dot is found, this is by definition the original FidoCad
			// standard library (immutable).
			
			if(dotPos<0)
				continue;
			String lib = md.key.substring(0,dotPos).trim();
			if (lib.equalsIgnoreCase(libfile)) {
				mm.put(e.getKey(), md);
			}
		}
		return mm;		
	}
	
	/** Prepare an header and collect text for creating a complete library.
		@param m the macro map associated to the library
		@param name the name of the library
		@return the library description in FidoCadJ code.
	
	*/
	public static String prepareText(Map<String,MacroDesc> m, String name) 
	{	
		StringBuffer sb = new StringBuffer();		
		String prev = null;
		int u;
		MacroDesc md;	
		// Header
    	sb.append("[FIDOLIB " + name + "]\n");
    	for (Entry<String,MacroDesc> e : m.entrySet()) {    		  
    		md = e.getValue();
    		// Category (check if it is changed)
    		if (prev == null || !prev.equalsIgnoreCase(md.category.trim())) {
    			sb.append("{"+md.category+"}\n"); 
    			prev = md.category.toLowerCase().trim(); 
    		}    		
    		sb.append("[");
    		// When the macros are written in the library, they contain only
    		// the last part of the key, since the first part (before the .)
    		// is always the file name.
    		sb.append(md.key.substring(
    			md.key.lastIndexOf(".")+1).toUpperCase().trim());
    		sb.append(" ");
    	    sb.append(md.name.trim());
    	    sb.append("]");
        	u = md.description.codePointAt(0) == '\n'?1:0;
        	sb.append("\n");
        	sb.append(md.description.substring(u)); 
    		sb.append("\n");
    	}
		return sb.toString();		
	}
	
	/** Save to a file a string respecting the global encoding settings.
		@param file the file name 
		@param the string to be written
		@return 
	*/
	public static void saveToFile(String file, String text) 
		throws FileNotFoundException
	{		
		System.out.println("file: "+file+"\n------\n"+text);
		PrintWriter pw;
		try {
			pw = new PrintWriter(file, Globals.encoding);
			pw.print(text);
			pw.flush();
			pw.close();
		} catch (UnsupportedEncodingException e) { 
			e.printStackTrace();
		}				
	}
	
	/** Save a library in a file.
		@param m the map containing the library.
		@param file the file name.
		@param libname the name of the library.
	*/
	public static void save(Map<String,MacroDesc> m, String file, 
		String libname) 
	{
		try {
			phylum_LibUtils.saveToFile(file + ".fcl", 
				phylum_LibUtils.prepareText(
				phylum_LibUtils.getLibrary(m, libname), libname));
		} catch (FileNotFoundException e) { 
			e.printStackTrace();
		}		
	}

	public static String getLibDir() throws FileNotFoundException
	{
		Preferences prefs = Preferences.userNodeForPackage(Globals.class);
		String s = prefs.get("DIR_LIBS", "");
		if (s == null || s.length()==0) {
			throw new FileNotFoundException();			
		}
		if (!s.endsWith(System.getProperty("file.separator"))) s+=System.getProperty("file.separator");		
		return s;		
	}
	
	/** Returns full path to lib file.
	  @param lib Library name.
	 */
	public static String getLibPath(String lib) throws FileNotFoundException 
	{		
		return getLibDir()+lib.trim();		
	}

	/** Eliminates a library.
		@param s Name of the library to eliminate
	*/
	public static void deleteLib(String  s) throws FileNotFoundException
	{
		File f = new File(getLibDir()+s+".fcl");
		f.delete();		
	}
	
	/** Get all the library in the current library directory.
		@return a list containing all the library files.
	
	*/
	public static List<File> getLibs() throws FileNotFoundException
	{
		File lst = new File(phylum_LibUtils.getLibDir());
		List<File> l = new ArrayList<File>();
		if (!lst.exists()) 
			return null;
		for (File f : lst.listFiles()) {
			if (f.getName().toLowerCase().endsWith(".fcl")) l.add(f);			
		}
		return l;				
	}
	
	/** Determine whether a library is standard or not.
		@param szlib the name (better prefix?) of the library
		@return true if the specified library is standard
	*/
	public static boolean isStdLib(MacroDesc tlib)
	{
		String szlib=tlib.library;
		String szfn=tlib.filename;
		
//		System.out.println("filename: "+szfn);
		
		if(szlib==null)
			return false;
		/*
		String[] libs = {"Standard library","Electrical symbols",
			"IHRAM 3.1","PCB Footprints",
			"Libreria standard","Simboli Elettrotecnica"};
			
		String[] files = {"ihram","elettrotecnica",
			"pcb","stdlib"};
			
		
		boolean check=false;
		
		
		for (String s : libs)
			if (s.toLowerCase().trim().equalsIgnoreCase(		
				szlib.toLowerCase().trim())) 
				check=true;
		
		for (String s : files)
			if (s.toLowerCase().trim().equalsIgnoreCase(		
				szfn.toLowerCase().trim())) 
				check=true;
		*/
		
		boolean isStandard=false;
		int dotpos=-1;
		boolean extensions=true;
		
		// A first way to determine if a macro is standard is to see if its
		// name does not contains a dot (original FidoCAD standard library)
		
		if ((dotpos=tlib.key.indexOf("."))<0) { 
			isStandard = true;
		} else {
			// If the name contains a dot, we might check whether we have 
			// one of the new FidoCadJ standard libraries:
			// pcb, ihram, elettrotecnica.
			
			// Obtain the library name
			String library=tlib.key.substring(0,dotpos);
			
			// Check it
			if(extensions && library.equals("pcb")) { 
				isStandard = true;
			} else if (extensions && library.equals("ihram")) {
				isStandard = true;
			} else if (extensions && library.equals("elettrotecnica")) {
				isStandard = true;
			}
		}
			
		return isStandard;
	}
	
	/** Rename a group inside a library
		@param libref the map containing the library
		@param tlib the name of the library
		@param tgrp the name of the group to be renamed
		@param newname the new name of the group
		
		DB: what if a group is not present?

	*/
	public static void renameGroup(Map<String, MacroDesc> libref, String tlib,
			String tgrp, String newname) throws FileNotFoundException
	{
		for (MacroDesc md : libref.values()) {
			if (md.category.equalsIgnoreCase(tgrp)
					&& md.library.trim().equalsIgnoreCase(
							tlib.trim()))
				md.category = newname;
		}
		save(libref, getLibPath(tlib), tlib.trim());
	}
	
	/** Check whether a key is used in a given library or it is available.
		Also check for strange characters.
		@param libref the map containing the library
		@param tlib the name of the library
		@param key the key to be checked
		@return false if the key is available, true if it is used.
	*/
	public static boolean checkKey(Map<String, MacroDesc> libref, 
		String tlib,String key) 
	{
		for (MacroDesc md : libref.values()) {
			if (md.library.equalsIgnoreCase(tlib)) {
				if(md.key.equalsIgnoreCase(key.trim()))
					return true;
			}
		}
		if(key.contains("]"))
			return true;
			
		return false;
	}
	
	/** Check if a library name is acceptable. Since the library name is used
		also as a file name, it must not contain characters which would 
		be in conflict with the rules of file names in the various operating
		systems.
		@return true if something strange is found.
	*/
	public static boolean checkLibrary(String library)
	{
		if(library.contains("]")||library.contains(".")||
		   library.contains("/")||library.contains("\\")||
		   library.contains("~")||library.contains("&")||
		   library.contains(",")||library.contains(";")||
		   library.contains("]")||library.contains("\""))
			return true;
		return false;
	}
	
	/** Save a library in a file
		@param m the map containing the library
		@param file the name of the file to be written
		@param libname the name to be used for the library
		@param libname2 library new name (if renamed)
	*/
	public static void save(Map<String, MacroDesc> m, String file,
			String libname, String libname2) 
	{
	
		System.out.println("file: "+file);
		try {
			String flibname = libname2;
			// Avoid modifying the standard library
			//MacroDesc tag= new MacroDesc("","","","",libname, file);
			//if (isStdLib(tag)) 
			//	flibname = "custom_" + libname;		
			file = file.replace(libname, flibname);					
			phylum_LibUtils.saveToFile(file + ".fcl", 
					   phylum_LibUtils.prepareText(
							   phylum_LibUtils.getLibrary(m, libname), 
							   libname2));
			deleteLib(libname);
		} catch (FileNotFoundException e) { 
			e.printStackTrace();
		}	
		
	}

	/** Delete a group inside a library
		@param m the map containing the library
		@param tlib the library name
		@param tgrp the group to be deleted.
		
		DB: what if a group is not found?
	
	*/
	public static void deleteGroup(Map<String, MacroDesc> m,String tlib, 
		String tgrp) throws FileNotFoundException
	{
		Map<String, MacroDesc> mm = new TreeMap<String, MacroDesc>();
		mm.putAll(m);
		for (Entry<String, MacroDesc> smd : mm.entrySet())
		{
			MacroDesc md = smd.getValue();			
			if (md.library.trim().equalsIgnoreCase(tlib) && 
					md.category.equalsIgnoreCase(tgrp)) m.remove(md.key);
		}
		save(m, getLibPath(tlib), tlib);
	}
	
	/** Obtain a list containing all the groups in a given library
		@m the map containing all the libraries
		@szlib the name of the wanted library
	*/
	public static List enumGroups(Map<String,MacroDesc> m, String szlib) 
	{
 		List lst = new LinkedList();
 		for (MacroDesc md : m.values()) {
		 	if (!lst.contains(md.category)
			 	&& szlib.trim().equalsIgnoreCase(md.library.trim()))  {
		 		lst.add(md.category);
		 	}
 		}
 		return lst;
	}


// TODO support libs with different filenames

public static String Languages[][] = {
{"af","Afrikaans"},
{"ak","Akan"},
{"sq","Albanian"},
{"am","Amharic"},
{"ar","Arabic"},
{"hy","Armenian"},
{"az","Azerbaijani"},
{"eu","Basque"},
{"be","Belarusian"},
{"bem","Bemba"},
{"bn","Bengali"},
{"bh","Bihari"},
//{"xx-bork","Bork, bork, bork!"},
{"bs","Bosnian"},
{"br","Breton"},
{"bg","Bulgarian"},
{"km","Cambodian"},
{"ca","Catalan"},
{"chr","Cherokee"},
{"ny","Chichewa"},
{"zh-CN","Chinese (Simplified)"},
{"zh-TW","Chinese (Traditional)"},
{"zh","Chinese"},
{"co","Corsican"},
{"hr","Croatian"},
{"cs","Czech"},
{"da","Danish"},
{"nl","Dutch"},
//{"xx-elmer","Elmer Fudd"},
{"en","English"},
{"eo","Esperanto"},
{"et","Estonian"},
{"ee","Ewe"},
{"fo","Faroese"},
{"tl","Filipino"},
{"fi","Finnish"},
{"fr","French"},
{"fy","Frisian"},
{"gaa","Ga"},
{"gl","Galician"},
{"ka","Georgian"},
{"de","German"},
{"el","Greek"},
{"gn","Guarani"},
{"gu","Gujarati"},
//{"xx-hacker,"Hacker"},
{"ht","Haitian Creole"},
{"ha","Hausa"},
{"haw","Hawaiian"},
{"iw","Hebrew"},
{"hi","Hindi"},
{"hu","Hungarian"},
{"is","Icelandic"},
{"ig","Igbo"},
{"id","Indonesian"},
{"ia","Interlingua"},
{"ga","Irish"},
{"it","Italiano"},
{"ja","Japanese"},
{"jw","Javanese"},
{"kn","Kannada"},
{"kk","Kazakh"},
{"rw","Kinyarwanda"},
{"rn","Kirundi"},
{"xx-klingon  Klingon"},
{"kg","Kongo"},
{"ko","Korean"},
{"kri","Krio (Sierra Leone)"},
{"ku","Kurdish"},
{"ckb","Kurdish (Soran�)"},
{"ky","Kyrgyz"},
{"lo","Laothian"},
{"la","Latin"},
{"lv","Latvian"},
{"ln","Lingala"},
{"lt","Lithuanian"},
{"loz","Lozi"},
{"lg","Luganda"},
{"ach","Luo"},
{"mk","Macedonian"},
{"mg","Malagasy"},
{"ms","Malay"},
{"ml","Malayalam"},
{"mt","Maltese"},
{"mi","Maori"},
{"mr","Marathi"},
{"mfe","Mauritian Creole"},
{"mo","Moldavian"},
{"mn","Mongolian"},
{"sr-ME","Montenegrin"},
{"ne","Nepali"},
{"pcm","Nigerian Pidgin"},
{"nso","Northern Sotho"},
{"no","Norwegian"},
{"nn","Norwegian (Nynorsk)"},
{"oc","Occitan"},
{"or","Oriya"},
{"om","Oromo"},
{"ps","Pashto"},
{"fa","Persian"},
//{"xx-pirate","Pirate"},
{"pl","Polish"},
{"pt-BR","Portuguese (Brazil)"},
{"pt-PT","Portuguese (Portugal)"},
{"pa","Punjabi"},
{"qu","Quechua"},
{"ro","Romanian"},
{"rm","Romansh"},
{"nyn","Runyakitara"},
{"ru","Russian"},
{"gd","Scots Gaelic"},
{"sr","Serbian"},
{"sh","Serbo-Croatian"},
{"st","Sesotho"},
{"tn","Setswana"},
{"crs","Seychellois Creole"},
{"sn","Shona"},
{"sd","Sindhi"},
{"si","Sinhalese"},
{"sk","Slovak"},
{"sl","Slovenian"},
{"so","Somali"},
{"es","Spanish"},
{"es-419","Spanish (Latin American)"},
{"su","Sundanese"},
{"sw","Swahili"},
{"sv","Swedish"},
{"tg","Tajik"},
{"ta","Tamil"},
{"tt","Tatar"},
{"te","Telugu"},
{"th","Thai"},
{"ti","Tigrinya"},
{"to","Tonga"},
{"lua","Tshiluba"},
{"tum","Tumbuka"},
{"tr","Turkish"},
{"tk","Turkmen"},
{"tw","Twi"},
{"ug","Uighur"},
{"uk","Ukrainian"},
{"ur","Urdu"},
{"uz","Uzbek"},
{"vi","Vietnamese"},
{"cy","Welsh"},
{"wo","Wolof"},
{"xh","Xhosa"},
{"yi","Yiddish"},
{"yo","Yoruba"},
{"zu","Zulu"}
};


	
}