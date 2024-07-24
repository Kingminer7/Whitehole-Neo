/*
 * Copyright (C) 2022 Whitehole Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package whitehole.smg;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import whitehole.Whitehole;
import whitehole.db.FieldHashes;
import whitehole.db.GalaxyNames;
import whitehole.db.ObjectDB;
import whitehole.io.ExternalFilesystem;
import whitehole.io.FilesystemBase;
import whitehole.io.RarcFile;

public class GameArchive {
    public static final String[] RESOURCE_FOLDERS = { "ObjectData", "LightData", "DemoData", "MapPartsData" };
    
    // -------------------------------------------------------------------------------------------------------------------------
    
    private final FilesystemBase filesystem;
    private List<String> galaxies = new ArrayList(64);
    private List<String> zones = new ArrayList(128);
    private List<String> planets = new ArrayList(256);
    private int gameType = 0;
    private boolean hasOverwriteObjectDatabase;
    private boolean hasOverwriteGalaxyNames;
    private boolean hasOverwriteZoneNames;
    private boolean hasOverwriteHints;
    private boolean hasOverwriteAreaManagerLimits;
    private boolean hasOverwriteSpecialRenderer;
    
    public GameArchive(FilesystemBase fs) {
        filesystem = fs;
        
        // Determine game type using ObjNameTable's location
        if (filesystem.fileExists("/StageData/ObjNameTable.arc")) {
            gameType = 1;
        }
        else if (filesystem.fileExists("/SystemData/ObjNameTable.arc")) {
            gameType = 2;
        }
        else {
            return;
        }
        
        // Initialize galaxy and zone list and collect hashes
        for (String stage : filesystem.getDirectories("/StageData")) {
            FieldHashes.add(stage);
            
            if (filesystem.fileExists(String.format("/StageData/%1$s/%1$sScenario.arc", stage))) {
                galaxies.add(stage);
            }
            
            if (gameType == 2 && filesystem.fileExists(String.format("/StageData/%1$s/%1$sMap.arc", stage))) {
                zones.add(stage);
            } 
        }
        
        // add zones for smg1
        if (gameType == 1) {
            for (String file : filesystem.getFiles("/StageData")) {
                if (file.endsWith("Zone.arc") || file.endsWith("Galaxy.arc")) {
                    zones.add(file.replace(".arc", ""));
                }
            }
        }
        
        // Initialize list of water planets
        try {
            RarcFile arc = new RarcFile(filesystem.openFile("/ObjectData/PlanetMapDataTable.arc"));
            Bcsv bcsv = new Bcsv(arc.openFile("/PlanetMapDataTable/PlanetMapDataTable.bcsv"));

            for (Bcsv.Entry entry : bcsv.entries) {
                if ((int)entry.get("WaterFlag") > 0) {
                    planets.add((String)entry.get("PlanetName"));
                }
            }

            bcsv.close();
            arc.close();
        }
        catch (IOException ex) {
            System.err.println(ex);
        }
        
        // Try to load project's overwrite databases
        if (filesystem instanceof ExternalFilesystem) {
            hasOverwriteObjectDatabase = ObjectDB.tryOverwriteWithProjectDatabase((ExternalFilesystem)filesystem);
            hasOverwriteGalaxyNames = Whitehole.GalaxyNames.initProject((ExternalFilesystem)filesystem);
            hasOverwriteZoneNames = Whitehole.ZoneNames.initProject((ExternalFilesystem)filesystem);
            hasOverwriteHints = Whitehole.Hints.initProject((ExternalFilesystem)filesystem);
            hasOverwriteAreaManagerLimits = Whitehole.AreaManagerLimits.initProject((ExternalFilesystem)filesystem);
            hasOverwriteSpecialRenderer = Whitehole.SpecialRenderers.initProject((ExternalFilesystem)filesystem);
        }
    }
    
    public void close() {
        try { 
            filesystem.close();
        }
        catch (IOException ex) {
            System.err.println(ex);
        }
    }
    
    // -------------------------------------------------------------------------------------------------------------------------
    
    public FilesystemBase getFileSystem() {
        return filesystem;
    }
    
    public int getGameType() {
        return gameType;
    }
    
    public boolean existsArchive(String filePath) {
        return filesystem.fileExists(filePath);
    }
    
    public List<String> getGalaxyList() {
        return galaxies;
    }
    
    public List<String> getZoneList() {
        return zones;
    }
    
    public List<String> getWaterPlanetList() {
        return planets;
    }
    
    public GalaxyArchive openGalaxy(String name) throws IOException {
        return galaxyExists(name) ? new GalaxyArchive(this, name) : null;
    }
    
    public boolean galaxyExists(String name) {
        return galaxies.contains(name);
    }
    
    public boolean existsResourceArcPath(String file) {
        return createResourceArcPath(file) != null;
    }
    
    public String createResourceArcPath(String objModelName) {
        String arcPath;
        
        for (String resourceFolder : RESOURCE_FOLDERS) {
            arcPath = String.format("/%s/%s.arc", resourceFolder, objModelName);
            
            if (filesystem.fileExists(arcPath)) {
                return arcPath;
            }
        }
        
        return null;
    }
    
    public boolean hasOverwriteObjectDatabase() {
        return hasOverwriteObjectDatabase;
    }
    
    public boolean hasOverwriteGalaxyNames() {
        return hasOverwriteGalaxyNames;
    }
    
    public boolean hasOverwriteZoneNames() {
        return hasOverwriteZoneNames;
    }
    
    public boolean hasOverwriteHints() {
        return hasOverwriteHints;
    }
    
    public boolean hasOverwriteAreaManagerLimits() {
        return hasOverwriteAreaManagerLimits;
    }
    
    public boolean hasOverwriteSpecialRenderer() {
        return hasOverwriteSpecialRenderer;
    }
}
