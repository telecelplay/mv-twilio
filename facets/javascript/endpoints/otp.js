const otp = async (parameters) =>  {
	const baseUrl = window.location.origin;
	const url = new URL(`${window.location.pathname.split('/')[1]}/rest/otp/${parameters.to}`, baseUrl);
	return fetch(url.toString(), {
		method: 'GET'
	});
}

const otpForm = (container) => {
	const html = `<form id='otp-form'>
		<div id='otp-to-form-field'>
			<label for='to'>to</label>
			<input type='text' id='otp-to-param' name='to'/>
		</div>
		<button type='button'>Test</button>
	</form>`;

	container.insertAdjacentHTML('beforeend', html)

	const to = container.querySelector('#otp-to-param');

	container.querySelector('#otp-form button').onclick = () => {
		const params = {
			to : to.value !== "" ? to.value : undefined
		};

		otp(params).then(r => r.text().then(
				t => alert(t)
			));
	};
}

export { otp, otpForm };