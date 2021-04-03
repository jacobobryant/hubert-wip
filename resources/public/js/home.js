const form = document.querySelector('form');

function authenticate(e) {
  form.action = '/hub/api/authenticate';
  form.method = 'POST';
  form.submit();
  e.preventDefault();
}

document.querySelector('form').addEventListener('submit', authenticate);
