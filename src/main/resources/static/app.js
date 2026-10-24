(() => {
  const API = '/api/people';
  const FIELDS = ['firstName', 'lastName', 'email', 'phone', 'dateOfBirth', 'address', 'city', 'country'];

  const form = document.getElementById('person-form');
  const submitBtn = document.getElementById('submit');
  const cancelBtn = document.getElementById('cancel');
  const formTitle = document.getElementById('form-title');
  const formError = document.getElementById('err-form');
  const table = document.getElementById('people-table');
  const tbody = document.getElementById('people-body');
  const empty = document.getElementById('empty');
  const count = document.getElementById('count');
  const toast = document.getElementById('toast');

  let people = [];
  let editingId = null;
  let toastTimer;

  // ---- API helper --------------------------------------------------------
  async function request(url, options = {}) {
    const res = await fetch(url, {
      credentials: 'same-origin', // sends the session cookie
      headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
      ...options,
    });
    if (res.status === 204) return null;
    const body = await res.json().catch(() => null);
    if (!res.ok) {
      const error = new Error((body && body.detail) || res.statusText);
      error.errors = (body && body.errors) || {};
      throw error;
    }
    return body;
  }

  // ---- UI helpers --------------------------------------------------------
  function showToast(message, isError = false) {
    toast.textContent = message;
    toast.classList.toggle('error', isError);
    toast.classList.add('show');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => toast.classList.remove('show'), 2600);
  }

  function clearErrors() {
    FIELDS.forEach((name) => {
      document.getElementById('err-' + name).textContent = '';
      form.elements[name].removeAttribute('aria-invalid');
    });
    formError.textContent = '';
  }

  function showErrors(errors, fallback) {
    let shown = false;
    Object.entries(errors).forEach(([name, message]) => {
      const slot = document.getElementById('err-' + name);
      if (slot) {
        slot.textContent = message;
        form.elements[name].setAttribute('aria-invalid', 'true');
        shown = true;
      }
    });
    if (!shown) formError.textContent = fallback;
    const firstBad = form.querySelector('[aria-invalid="true"]');
    if (firstBad) firstBad.focus();
  }

  function readForm() {
    const data = {};
    FIELDS.forEach((name) => {
      const value = form.elements[name].value.trim();
      data[name] = value === '' ? null : value;
    });
    return data;
  }

  function startEdit(person) {
    editingId = person.id;
    FIELDS.forEach((name) => { form.elements[name].value = person[name] || ''; });
    formTitle.textContent = 'Edit ' + person.firstName;
    submitBtn.textContent = 'Save changes';
    cancelBtn.hidden = false;
    clearErrors();
    render();
    form.elements.firstName.focus();
  }

  function resetForm() {
    editingId = null;
    form.reset();
    formTitle.textContent = 'Add a person';
    submitBtn.textContent = 'Save person';
    cancelBtn.hidden = true;
    clearErrors();
    render();
  }

  // ---- Rendering (textContent only, so stored values can never inject HTML) ----
  function cell(text, sub) {
    const td = document.createElement('td');
    td.textContent = text || '';
    if (sub) {
      const small = document.createElement('span');
      small.className = 'sub';
      small.textContent = sub;
      td.appendChild(small);
    }
    return td;
  }

  function actionButton(label, className, handler) {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'link ' + className;
    button.textContent = label;
    button.addEventListener('click', handler);
    return button;
  }

  function render() {
    tbody.replaceChildren();
    people.forEach((person) => {
      const tr = document.createElement('tr');
      if (person.id === editingId) tr.className = 'editing';

      tr.appendChild(cell(person.firstName + ' ' + person.lastName, person.dateOfBirth ? 'Born ' + person.dateOfBirth : ''));
      tr.appendChild(cell(person.email));
      tr.appendChild(cell(person.phone));
      tr.appendChild(cell([person.city, person.country].filter(Boolean).join(', '), person.address));

      const actions = document.createElement('td');
      actions.className = 'actions-cell';
      actions.appendChild(actionButton('Edit', '', () => startEdit(person)));
      actions.appendChild(actionButton('Delete', 'danger', () => remove(person)));
      tr.appendChild(actions);
      tbody.appendChild(tr);
    });

    const has = people.length > 0;
    table.hidden = !has;
    empty.hidden = has;
    count.textContent = has ? people.length + (people.length === 1 ? ' person' : ' people') : '';
  }

  // ---- Actions -----------------------------------------------------------
  async function load() {
    try {
      people = await request(API);
      render();
    } catch (e) {
      showToast('Could not load saved people: ' + e.message, true);
    }
  }

  async function remove(person) {
    if (!confirm('Delete ' + person.firstName + ' ' + person.lastName + '?')) return;
    try {
      await request(API + '/' + person.id, { method: 'DELETE' });
      people = people.filter((p) => p.id !== person.id);
      if (editingId === person.id) resetForm(); else render();
      showToast('Deleted');
    } catch (e) {
      showToast(e.message, true);
    }
  }

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    clearErrors();
    submitBtn.disabled = true;
    const wasEditing = editingId !== null;
    try {
      const payload = JSON.stringify(readForm());
      if (wasEditing) {
        const saved = await request(API + '/' + editingId, { method: 'PUT', body: payload });
        people = people.map((p) => (p.id === saved.id ? saved : p));
      } else {
        people = [...people, await request(API, { method: 'POST', body: payload })];
      }
      resetForm();
      showToast(wasEditing ? 'Changes saved' : 'Saved');
    } catch (e) {
      showErrors(e.errors || {}, e.message);
    } finally {
      submitBtn.disabled = false;
    }
  });

  cancelBtn.addEventListener('click', resetForm);

  // ---- Deployed version stamp (reads /actuator/info) -----------------------
  async function loadStamp() {
    const stamp = document.getElementById('stamp');
    try {
      const res = await fetch('/actuator/info', { headers: { Accept: 'application/json' } });
      const info = await res.json();
      const version = info.build && info.build.version;
      const commit = info.app && info.app.commit;
      const short = commit && commit !== 'unknown' ? commit.slice(0, 7) : null;
      if (version || short) {
        stamp.textContent = 'Running ' + (version ? 'v' + version : 'build') + (short ? ', commit ' + short : ', local build');
      } else {
        stamp.textContent = 'Version unavailable';
        stamp.classList.add('unknown');
      }
    } catch (e) {
      stamp.textContent = 'Version unavailable';
      stamp.classList.add('unknown');
    }
  }

  load();
  loadStamp();
})();
