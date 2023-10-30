package com.discord;

import com.google.gson.*;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;
import java.util.Properties;

public class DiscordBot {
	public static void main( String[] args ) throws IOException {
		JsonArray mods;
		try {
			mods = JsonParser.parseReader( new FileReader( "src/main/resources/mods.json" ) ).getAsJsonArray();
		} catch( Exception exception ) {
			exception.printStackTrace();
			return;
		}
		JsonObject message = new JsonObject();
		message.addProperty( "content", "@here" );

		Properties properties = new Properties();
		JsonArray embeds = new JsonArray();
		for( JsonElement modElement : mods ) {
			JsonObject mod = modElement.getAsJsonObject();
			try {
				properties.load( new InputStreamReader( new URL( mod.get( "properties_url" ).getAsString() ).openStream() ) );
			} catch( Exception exception ) {
				exception.printStackTrace();
				return;
			}

			Type type = Type.of( mod.get( "type" ).getAsString() );
			String description = "Minecraft %s".formatted( properties.getProperty( "minecraft_version" ) );
			if( mod.has( "platforms" ) ) {
				description += " for %s".formatted( mod.get( "platforms" ).getAsString() );
			}
			description += "\n\n";
			description += "Download links:\n";
			for( JsonElement downloadElement : mod.getAsJsonArray( "downloads" ) ) {
				JsonObject download = downloadElement.getAsJsonObject();
				description += "[%s](%s)\n".formatted( download.get( "name" ).getAsString(), download.get( "url" ).getAsString() );
			}
			description += "\n";
			if( mod.has( "wiki_url" ) ) {
				description += "Remember to check out the [wiki](%s)!".formatted( mod.get( "wiki_url" ).getAsString() );
				description += "\n\n";
			}
			if( mod.has( "changelog_url" ) ) {
				description += "Changelog:";
				description += "\n";
				try( InputStream stream = new URL( mod.get( "changelog_url" ).getAsString() ).openStream() ) {
					String line;
					BufferedReader reader = new BufferedReader( new InputStreamReader( stream ) );
					while( ( line = reader.readLine() ) != null ) {
						description += line + "\n";
					}
				}
			}

			JsonObject embed = new JsonObject();
			embed.addProperty( "title", "%s %s is now available!".formatted( properties.getProperty( "mod_display_name" ), properties.getProperty( "mod_version" ) ) );
			embed.addProperty( "description", description );
			embed.addProperty( "color", type.color );

			embeds.add( embed );
		}

		message.add( "embeds", embeds );
		message.add( "attachments", new JsonArray() );

		try {
			URL url = new URL( System.getenv( "DISCORD_ANNOUNCEMENT_HOOK" ) );
			HttpsURLConnection connection = ( HttpsURLConnection )url.openConnection();
			connection.addRequestProperty( "Content-Type", "application/json" );
			connection.addRequestProperty( "User-Agent", "Java-DiscordWebhook" );
			connection.setDoOutput( true );
			connection.setRequestMethod( "POST" );
			OutputStream stream = connection.getOutputStream();
			stream.write( new Gson().toJson( message ).getBytes() );
			stream.flush();
			stream.close();
			connection.getInputStream().close();
			connection.disconnect();
		} catch( Exception exception ) {
			exception.printStackTrace();
		}
	}

	private enum Type {
		MAJOR( "MAJOR", "16734296" ),
		MINOR( "MINOR", "5832536" ),
		PATCH( "PATCH", "5814783" );

		private final String name;
		private final String color;

		public static Type of( String name ) {
			for( Type type : Type.values() ) {
				if( type.name.equals( name ) ) {
					return type;
				}
			}

			return PATCH;
		}

		private Type( String name, String color ) {
			this.name = name;
			this.color = color;
		}
	}
}