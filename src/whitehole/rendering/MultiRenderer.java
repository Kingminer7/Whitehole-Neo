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
package whitehole.rendering;

import com.jogamp.opengl.GL2;
import java.util.List;
import whitehole.util.Vector3;

public class MultiRenderer extends GLRenderer {
    private static final Vector3 TRANSLATION = new Vector3(0f, 0f, 0f);
    private static final Vector3 ROTATION = new Vector3(0f, 0f, 0f);
    private static final Vector3 SCALE = new Vector3(1f, 1f, 1f);
    
    public static class MultiRendererInfo {
        String modelName;
        Vector3 position, rotation, scale;
        GLRenderer renderer;
        
        public MultiRendererInfo(String modelname) {
            modelName = modelname;
            position = TRANSLATION;
            rotation = ROTATION;
            scale = SCALE;
        }
        
        public MultiRendererInfo(String modelname, Vector3 pos) {
            modelName = modelname;
            position = pos;
            rotation = ROTATION;
            scale = SCALE;
        }
        
        public MultiRendererInfo(String modelname, Vector3 pos, Vector3 dir) {
            modelName = modelname;
            position = pos;
            rotation = dir;
            scale = SCALE;
        }
        
        public MultiRendererInfo(String modelname, Vector3 pos, Vector3 dir, Vector3 size) {
            modelName = modelname;
            position = pos;
            rotation = dir;
            scale = size;
        }
    }
    
    // -------------------------------------------------------------------------------------------------------------------------
    
    private final List<MultiRendererInfo> submodelRenderers;
    
    MultiRenderer(List<MultiRendererInfo> subRenderers) {
        submodelRenderers = subRenderers;
    }
    
    @Override
    public void close(RenderInfo info) {
        for (MultiRendererInfo multiInfo : submodelRenderers) {
            multiInfo.renderer.close(info);
        }
    }
    
    @Override
    public boolean gottaRender(RenderInfo info) {
        boolean ret = false;
        
        for (MultiRendererInfo multiInfo : submodelRenderers) {
            ret |= multiInfo.renderer.gottaRender(info);
        }
        
        return ret;
    }
    
    @Override
    public void render(RenderInfo info) {
        GL2 gl = info.drawable.getGL().getGL2();
        
        for (MultiRendererInfo multiInfo : submodelRenderers) {
            if (!multiInfo.renderer.gottaRender(info)) {
                continue;
            }
            
            Vector3 translation = multiInfo.position;
            Vector3 rotation = multiInfo.rotation;
            Vector3 scale = multiInfo.scale;
            
            gl.glTranslatef(translation.x, translation.y, translation.z);
            gl.glRotatef(rotation.x, 0f, 0f, 1f);
            gl.glRotatef(rotation.y, 0f, 1f, 0f);
            gl.glRotatef(rotation.z, 1f, 0f, 0f);
            gl.glScalef(scale.x, scale.y, scale.z);
            
            multiInfo.renderer.render(info);
        }
    }
}
