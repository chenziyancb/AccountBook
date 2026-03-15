import struct, zlib, os

def create_simple_png(width, height, color_rgb, filename):
    def png_chunk(chunk_type, data):
        c = chunk_type + data
        return struct.pack('>I', len(data)) + c + struct.pack('>I', zlib.crc32(c) & 0xffffffff)
    
    r, g, b = color_rgb
    raw = b''
    for y in range(height):
        row = b'\x00'
        for x in range(width):
            cx = x - width//2
            cy = y - height//2
            dist = (cx*cx + cy*cy) ** 0.5
            radius = min(width, height) // 2 - 2
            if dist <= radius:
                row += bytes([r, g, b, 255])
            else:
                row += bytes([0, 0, 0, 0])
        raw += row
    
    compressed = zlib.compress(raw)
    signature = b'\x89PNG\r\n\x1a\n'
    ihdr_data = struct.pack('>IIBBBBB', width, height, 8, 6, 0, 0, 0)
    ihdr = png_chunk(b'IHDR', ihdr_data)
    idat = png_chunk(b'IDAT', compressed)
    iend = png_chunk(b'IEND', b'')
    with open(filename, 'wb') as f:
        f.write(signature + ihdr + idat + iend)

sizes = {'mipmap-mdpi': 48, 'mipmap-hdpi': 72, 'mipmap-xhdpi': 96, 'mipmap-xxhdpi': 144, 'mipmap-xxxhdpi': 192}
base = r'C:\Users\Administrator\WorkBuddy\Claw\app\src\main\res'

for folder, size in sizes.items():
    d = os.path.join(base, folder)
    os.makedirs(d, exist_ok=True)
    create_simple_png(size, size, (33, 150, 243), os.path.join(d, 'ic_launcher.png'))
    create_simple_png(size, size, (33, 150, 243), os.path.join(d, 'ic_launcher_round.png'))
    print(f'Created {folder}/ic_launcher.png ({size}x{size})')

print('Done!')
