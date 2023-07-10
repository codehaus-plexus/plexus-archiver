rm symlinks.zip
rm symlinks.tar
cd src
zip --symlinks ../symlinks.zip file* targetDir sym*
tar -cvf ../symlinks.tar file* targetDir sym*

cd ..
rm non_existing_symlink.zip
mkdir non_existing_symlink
cd non_existing_symlink
ln -s /tmp/target entry1
echo -ne 'content' > entry2
zip  --symlinks ../non_existing_symlink.zip entry1 entry2
cd ..
rm -rf non_existing_symlink
LC_ALL=C sed  -i '' 's/entry2/entry1/' non_existing_symlink.zip