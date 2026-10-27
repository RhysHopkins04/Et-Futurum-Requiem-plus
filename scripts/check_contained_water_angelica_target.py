#!/usr/bin/env python3
"""Check the optional batch hook against an actual Angelica distribution jar."""
import argparse
from pathlib import Path
import subprocess
import zipfile

OWNER='com/gtnewhorizons/angelica/rendering/celeritas/AngelicaChunkBuildContext'
MESHER='com.gtnewhorizons.angelica.rendering.celeritas.AngelicaChunkBuilderMeshingTask'
DESCRIPTOR='([II[I[ILorg/embeddedt/embeddium/impl/render/chunk/compile/ChunkBuildBuffers;Lorg/embeddedt/embeddium/impl/render/chunk/terrain/material/Material;ZZ)V'


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('angelica_jar',type=Path)
    parser.add_argument('--javap',default='javap',help='Use JDK 17+ javap for multi-release classes')
    args=parser.parse_args()
    def inspect(name,version=None):
        cmd=[args.javap]
        if version:cmd+=['--multi-release',str(version)]
        return subprocess.run(cmd+['-classpath',str(args.angelica_jar),'-p','-c',name],check=True,capture_output=True,text=True).stdout
    with zipfile.ZipFile(args.angelica_jar) as jar:
        prefix=MESHER.replace('.','/')+'.class'
        versions=[None]+sorted({int(n.split('/')[2]) for n in jar.namelist() if n.startswith('META-INF/versions/') and n.endswith('/'+prefix)})
    call='Method '+OWNER+'.copyRawBuffer:'+DESCRIPTOR
    for version in versions:
        source=inspect(MESHER,version)
        start=source.index('protected void renderBlock(')
        body=source[start:source.index('\n  protected ',start+1)] if '\n  protected ' in source[start+1:] else source[start:]
        # Limit the check to this method rather than deferred-task callsites.
        end=body.find('\n  private ')
        if end>=0:body=body[:end]
        assert body.count(call)==1,('Unexpected fluid-copy call layout',version)
        print('PASS fluid batch hook target:',version or 'base')
        copy_source=inspect(OWNER.replace('/','.'),version)
        copy_start=copy_source.index('public void copyRawBuffer(')
        copy_body=copy_source[copy_start:copy_source.index('\n  private void calculateQuadLight(',copy_start)]
        assert copy_body.count('Method calculateQuadLight:(IIIIIIIZZZ)V')==2,('Unexpected water-light calculation call layout',version)
        assert 'private final org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexEncoder$Vertex[] vertices;' in copy_source
        assert 'private final org.embeddedt.embeddium.impl.model.light.data.QuadLightData quadLightData;' in copy_source
        print('PASS water skylight hook targets and private data fields:',version or 'base')
    context=inspect(OWNER.replace('/','.'))
    assert 'public com.gtnewhorizons.angelica.rendering.celeritas.iris.BlockRenderContext getBlockRenderContext();' in context
    fields=inspect('com.gtnewhorizons.angelica.rendering.celeritas.iris.BlockRenderContext')
    assert 'public short renderType;' in fields
    encoder=inspect('com.gtnewhorizons.angelica.rendering.celeritas.iris.IrisExtendedChunkVertexEncoder')
    start=encoder.index('public void prepareToRenderFluid(')
    fluid=encoder[start:]
    assert 'iconst_1' in fluid[:1800] and 'BlockRenderContext.renderType:S' in fluid[:1800]
    print('PASS public fluid-context API and native fluid render type')

if __name__=='__main__':main()
