const verifOtp = async (parameters) =>  {
	const baseUrl = window.location.origin;
	const url = new URL(`${window.location.pathname.split('/')[1]}/rest/verifOtp/${parameters.to}`, baseUrl);
	return fetch(url.toString(), {
		method: 'POST', 
		headers : new Headers({
 			'Content-Type': 'application/json'
		}),
		body: JSON.stringify({
			code : parameters.code
		})
	});
}

const verifOtpForm = (container) => {
	const html = `<form id='verifOtp-form'>
		<div id='verifOtp-to-form-field'>
			<label for='to'>to</label>
			<input type='text' id='verifOtp-to-param' name='to'/>
		</div>
		<div id='verifOtp-code-form-field'>
			<label for='code'>code</label>
			<input type='text' id='verifOtp-code-param' name='code'/>
		</div>
		<button type='button'>Test</button>
	</form>`;

	container.insertAdjacentHTML('beforeend', html)

	const to = container.querySelector('#verifOtp-to-param');
	const code = container.querySelector('#verifOtp-code-param');

	container.querySelector('#verifOtp-form button').onclick = () => {
		const params = {
			to : to.value !== "" ? to.value : undefined,
			code : code.value !== "" ? code.value : undefined
		};

		verifOtp(params).then(r => r.text().then(
				t => alert(t)
			));
	};
}

export { verifOtp, verifOtpForm };