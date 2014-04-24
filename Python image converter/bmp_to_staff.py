import Image
import sys

filename = str(sys.argv[1])

imFile = Image.open(filename + ".bmp")
imFile = imFile.convert("RGB")
imStaff = open(filename + ".staff", 'w')

imwidth, imheight = imFile.size

for x in range(0, imwidth):
	for y in range(0,imheight):
		rgb = imFile.getpixel((x,y))
		imStaff.write(str(rgb[0])+','+str(rgb[1])+','+str(rgb[2]))
		if y != imheight-1:
			imStaff.write(',')
	imStaff.write("\n")