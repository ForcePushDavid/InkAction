import os

def fix_syntax(path):
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    content = content.replace('text = Žádné úkoly"', 'text = "Žádné úkoly"')
    content = content.replace('text = Aktivní úkoly"', 'text = "Aktivní úkoly"')
    content = content.replace('text = Historie (Dokončené)"', 'text = "Historie (Dokončené)"')
    
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)

fix_syntax(r'app/src/main/java/com/inkaction/app/ui/screens/TodosScreen.kt')
