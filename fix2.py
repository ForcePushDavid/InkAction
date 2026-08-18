import os
import re

def fix_file(path):
    with open(path, 'r', encoding='utf-8-sig', errors='ignore') as f:
        content = f.read()
    
    # We use regex to catch all possible garbled representations
    content = re.sub(r'Syntetizov.*?no', 'Syntetizováno', content)
    content = re.sub(r'Nov.*?pozn.*?mka', 'Nová poznámka', content)
    content = re.sub(r'.*?dn.*? .*?koly', 'Žádné úkoly', content)
    content = re.sub(r'Aktivn.*? .*?koly', 'Aktivní úkoly', content)
    content = re.sub(r'Dokon.*?en.*?\)', 'Dokončené)', content)
    
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)

fix_file(r'app/src/main/java/com/inkaction/app/ui/screens/NotesScreen.kt')
fix_file(r'app/src/main/java/com/inkaction/app/ui/screens/TodosScreen.kt')
