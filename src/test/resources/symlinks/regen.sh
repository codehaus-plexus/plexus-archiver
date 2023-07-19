rm symlinks.zip
rm symlinks.tar
cd src
zip --symlinks ../symlinks.zip file* targetDir sym*
tar -cvf ../symlinks.tar file* targetDir sym*
rm hardlink
ln fileR.txt hardlink
tar -cvf ../../hardlinks/hardlinks.tar fileR.txt hardlink
