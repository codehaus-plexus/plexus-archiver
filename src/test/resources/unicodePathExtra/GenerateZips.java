import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.compress.archivers.zip.UnicodePathExtraField;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;

public class GenerateZips {
	
	private static void generate(String outfilename, boolean languageEncodingFlag) {
		try {
			ZipArchiveOutputStream zs = new ZipArchiveOutputStream(new File(outfilename));
			zs.setUseLanguageEncodingFlag(languageEncodingFlag);
			zs.setMethod(ZipArchiveOutputStream.STORED);
			ZipArchiveEntry ze = new ZipArchiveEntry("nameonly-name");
			ze.setTime(0);
			zs.putArchiveEntry(ze);
			//zs.write(nothing);
			zs.closeArchiveEntry();
			ze = new ZipArchiveEntry("goodextra-name");
			ze.setTime(0);
			ze.addExtraField(new UnicodePathExtraField("goodextra-extra", "goodextra-name".getBytes(Charset.forName("UTF-8"))));
			zs.putArchiveEntry(ze);
			//zs.write(nothing);
			zs.closeArchiveEntry();
			ze = new ZipArchiveEntry("badextra-name");
			ze.setTime(0);
			ze.addExtraField(new UnicodePathExtraField("badextra-extra", "bogus".getBytes(Charset.forName("UTF-8"))));
			zs.putArchiveEntry(ze);
			//zs.write(nothing);
			zs.closeArchiveEntry();
			zs.finish();
			zs.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		generate("efsclear.zip", false);
		// with the flag set, decoders tend to not look at the extra field
		generate("efsset.zip", true);
		System.out.println("done");
	}

}
