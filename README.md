# PoMoDoRo

Android project "PoMoDoRo".

Как запушить проект на GitHub

1. Создайте репозиторий на GitHub (через веб-интерфейс или с помощью `gh`):
   - Через веб: https://github.com/new
   - Через gh CLI: `gh repo create USER/REPO --public`

2. В корне проекта выполните (PowerShell):

```powershell
cd C:\Users\STRNGBBY\AndroidStudioProjects\PoMoDoRo
# настроить имя и email, если ещё не настроены
git config user.name "Ваше Имя"
git config user.email "you@example.com"

# инициализировать репозиторий
git init
git add .
git commit -m "Initial commit"

# указать основную ветку и добавить remote (замените USER/REPO)
git branch -M main
# HTTPS
git remote add origin https://github.com/USER/REPO.git
# или SSH
# git remote add origin git@github.com:USER/REPO.git

git push -u origin main
```

Если удалённый репозиторий уже содержит коммиты (например, README.md создан через веб), выполните:

```powershell
# подтянуть и слить истории, если необходимо
git pull origin main --allow-unrelated-histories
git push -u origin main
```

Аутентификация

- При использовании HTTPS потребуется Personal Access Token (PAT) вместо пароля. Создайте PAT на https://github.com/settings/tokens и используйте его при запросе пароля.
- При использовании SSH — добавьте ваш публичный ключ (обычно `~/.ssh/id_rsa.pub`) в GitHub Settings -> SSH and GPG keys.

Если нужно — могу добавить инструкции для `gh` или настроить SSH ключи; скажите, что предпочитаете: HTTPS (с PAT) или SSH.
