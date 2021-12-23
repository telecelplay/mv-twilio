const outboundSMS = async (parameters) =>  {
	const baseUrl = window.location.origin;
	const url = new URL(`${window.location.pathname.split('/')[1]}/rest/outboundSMS/${parameters.result}`, baseUrl);
	return fetch(url.toString(), {
		method: 'POST', 
		headers : new Headers({
 			'Content-Type': 'application/json'
		}),
		body: JSON.stringify({
			to : parameters.to
		})
	});
}

const outboundSMSForm = (container) => {
	const html = `<form id='outboundSMS-form'>
		<div id='outboundSMS-result-form-field'>
			<label for='result'>result</label>
			<input type='text' id='outboundSMS-result-param' name='result'/>
		</div>
		<div id='outboundSMS-to-form-field'>
			<label for='to'>to</label>
			<input type='text' id='outboundSMS-to-param' name='to'/>
		</div>
		<button type='button'>Test</button>
	</form>`;

	container.insertAdjacentHTML('beforeend', html)

	const result = container.querySelector('#outboundSMS-result-param');
	const to = container.querySelector('#outboundSMS-to-param');

	container.querySelector('#outboundSMS-form button').onclick = () => {
		const params = {
			result : result.value !== "" ? result.value : undefined,
			to : to.value !== "" ? to.value : undefined
		};

		outboundSMS(params).then(r => r.text().then(
				t => alert(t)
			));
	};
}

export { outboundSMS, outboundSMSForm };