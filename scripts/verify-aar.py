import io
import struct
import sys
import zipfile

abis = {
    "arm64-v8a": (2, 183),
    "armeabi-v7a": (1, 40),
    "x86_64": (2, 62),
    "x86": (1, 3),
}

with zipfile.ZipFile(sys.argv[1]) as aar:
    with zipfile.ZipFile(io.BytesIO(aar.read("classes.jar"))) as classes:
        for name in ("Doltlite", "CDoltlite"):
            classes.getinfo(f"com/dolthub/doltlite/{name}.class")

    for abi, (elf_class, machine) in abis.items():
        with aar.open(f"jni/{abi}/libdoltlite.so") as library:
            header = library.read(20)
        if (header[:6] != b"\x7fELF" + bytes((elf_class, 1))
                or len(header) < 20 or struct.unpack_from("<H", header, 18)[0] != machine):
            raise SystemExit(f"libdoltlite.so does not match {abi}")

print("AAR contains both binding classes and all four matching Android ELF libraries")
