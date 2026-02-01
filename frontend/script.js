document.getElementById('expenseForm').addEventListener('submit', async function (e) {
  e.preventDefault();

  // Get form input values
  const group = document.getElementById('groupName').value;
  const payer = document.getElementById('payer').value;
  const amount = parseFloat(document.getElementById('amount').value);
  const participants = document.getElementById('participants').value.trim().split(" ");
  const description = document.getElementById('description').value;

  const data = { group, payer, amount, participants, description };

  try {
    // Send expense to backend
    await fetch('http://localhost:8000/add-expense', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    });

    // Fetch updated balances and settlements with group query
    const balancesResponse = await fetch(`http://localhost:8000/balances?group=${group}`);
    const balances = await balancesResponse.json();

    const settlementsResponse = await fetch(`http://localhost:8000/settlements?group=${group}`);
    const settlements = await settlementsResponse.json();

    // Display on UI
    document.getElementById('balances').textContent = JSON.stringify(balances, null, 2);
    document.getElementById('settlements').textContent = settlements.join('\n');
  } catch (error) {
    console.error("Failed to fetch or submit expense:", error);
    alert("Something went wrong. Please check your server or network connection.");
  }
});
