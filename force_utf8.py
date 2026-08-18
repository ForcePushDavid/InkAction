import os

def force_utf8(directory):
    for root, dirs, files in os.walk(directory):
        for file in files:
            if file.endswith('.kt') or file.endswith('.xml'):
                path = os.path.join(root, file)
                try:
                    # Read using utf-8-sig to strip BOM if present, or fallback to latin-1
                    try:
                        with open(path, 'r', encoding='utf-8-sig') as f:
                            content = f.read()
                    except UnicodeDecodeError:
                        with open(path, 'r', encoding='latin-1') as f:
                            content = f.read()
                    
                    # Write strictly as UTF-8 without BOM
                    with open(path, 'w', encoding='utf-8') as f:
                        f.write(content)
                except Exception as e:
                    print(f"Failed to process {path}: {e}")

force_utf8('app/src/main')
print("All files processed and forced to UTF-8.")
