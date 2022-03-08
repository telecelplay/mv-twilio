const verifyOtp = async (parameters) =>  {
	const baseUrl = window.location.origin;
	const url = new URL(`${window.location.pathname.split('/')[1]}/rest/verifyOtp/${parameters.to}`, baseUrl);
	return fetch(url.toString(), {
		method: 'POST', 
		headers : new Headers({
 			'Content-Type': 'application/json'
		}),
		body: JSON.stringify({
			otp : parameters.otp
		})
	});
}

const verifyOtpForm = (container) => {
	const html = `<form id='verifyOtp-form'>
		<div id='verifyOtp-to-form-field'>
			<label for='to'>to</label>
			<input type='text' id='verifyOtp-to-param' name='to'/>
		</div>
		<div id='verifyOtp-otp-form-field'>
			<label for='otp'>otp</label>
			<input type='text' id='verifyOtp-otp-param' name='otp'/>
		</div>
		<button type='button'>Test</button>
	</form>`;

	container.insertAdjacentHTML('beforeend', html)

	const to = container.querySelector('#verifyOtp-to-param');
	const otp = container.querySelector('#verifyOtp-otp-param');

	container.querySelector('#verifyOtp-form button').onclick = () => {
		const params = {
			to : to.value !== "" ? to.value : undefined,
			otp : otp.value !== "" ? otp.value : undefined
		};

		verifyOtp(params).then(r => r.text().then(
				t => alert(t)
			));
	};
}

export { verifyOtp, verifyOtpForm };