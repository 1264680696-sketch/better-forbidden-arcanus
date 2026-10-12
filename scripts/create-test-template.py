"""Write a minimal empty structure using the standard gzip NBT format."""
import gzip
from pathlib import Path
import struct

def string(value):
    raw = value.encode('utf-8')
    return struct.pack('>H', len(raw)) + raw

def named(kind, name, value):
    return bytes([kind]) + string(name) + value

payload = named(3, 'DataVersion', struct.pack('>i', 3955))
payload += named(9, 'size', bytes([3]) + struct.pack('>iiii', 3, 3, 3, 3))
payload += named(9, 'palette', bytes([10]) + struct.pack('>i', 1) + named(8, 'Name', string('minecraft:air')) + b'\x00')
payload += named(9, 'blocks', bytes([10]) + struct.pack('>i', 0))
payload += named(9, 'entities', bytes([10]) + struct.pack('>i', 0))
root = b'\x0a' + string('') + payload + b'\x00'
destination = Path(__file__).resolve().parents[1] / 'src/gametest/resources/data/bfa_tests/structure/empty.nbt'
destination.parent.mkdir(parents=True, exist_ok=True)
destination.write_bytes(gzip.compress(root, mtime=0))
print(destination)
